package net.macdidi.myandroidtutorial;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends Activity {

    // 移除原來的ListView元件
    //private ListView item_list;

    // 加入下列需要的元件
    private RecyclerView item_list;
    private RecyclerView.Adapter itemAdapter;
    private RecyclerView.LayoutManager rvLayoutManager;

    private TextView show_app_name;

    // 移除原來的ItemAdapter
    //private ItemAdapter itemAdapter;
    // 儲存所有記事本的List物件
    private List<Item> items;

    // 選單項目物件
    private MenuItem add_item, search_item, revert_item, delete_item;

    // 已選擇項目數量
    private int selectedCount = 0;

    // 宣告資料庫功能類別欄位變數
    private ItemDAO itemDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        processViews();
        // 移除註冊監聽事件的工作，要移到下面執行
        // processControllers();

        // 建立資料庫物件
        itemDAO = new ItemDAO(getApplicationContext());

        // 如果資料庫是空的，就建立一些範例資料
        // 這是為了方便測試用的，完成應用程式以後可以拿掉
        if (itemDAO.getCount() == 0) {
            itemDAO.sample();
        }

        // 取得所有記事資料
        items = itemDAO.getAll();

        // 移除原來ListView元件執行的工作
        //itemAdapter = new ItemAdapter(this, R.layout.single_item, items);
        //item_list.setAdapter(itemAdapter);

        // 執行RecyclerView元件的設定
        item_list.setHasFixedSize(true);
        rvLayoutManager = new LinearLayoutManager(this);
        item_list.setLayoutManager(rvLayoutManager);

        // 在這裡執行註冊監聽事件的工作
        processControllers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Item item = (Item) data.getExtras().getSerializable(
                    "net.macdidi.myandroidtutorial.Item");

            // 是否修改提醒設定
            boolean updateAlarm = false;

            if (requestCode == 0) {
                // 新增記事資料到資料庫
                item = itemDAO.insert(item);

                items.add(item);
                itemAdapter.notifyDataSetChanged();

                updateAlarm = true;
            }
            else if (requestCode == 1) {
                int position = data.getIntExtra("position", -1);

                if (position != -1) {
                    // 讀取原來的提醒設定
                    Item ori = itemDAO.get(item.getId());
                    // 判斷是否需要設定提醒
                    updateAlarm = (item.getAlarmDatetime() != ori.getAlarmDatetime());

                    itemDAO.update(item);
                    items.set(position, item);
                    itemAdapter.notifyDataSetChanged();
                }
            }

            // 設定提醒
            if (item.getAlarmDatetime() != 0 && updateAlarm) {
                Intent intent = new Intent(this, AlarmReceiver.class);
                // 移除原來的記事標題資料
                //intent.putExtra("title", item.getTitle());

                // 加入記事編號資料
                intent.putExtra("id", item.getId());

                PendingIntent pi = PendingIntent.getBroadcast(
                        this, (int)item.getId(),
                        intent, PendingIntent.FLAG_ONE_SHOT);

                AlarmManager am = (AlarmManager)
                        getSystemService(Context.ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, item.getAlarmDatetime(), pi);
            }
        }
    }

    private void processViews() {
        // 把ListViewe改為RecyclerView
        item_list = (RecyclerView)findViewById(R.id.item_list);
        show_app_name = (TextView) findViewById(R.id.show_app_name);
    }

    private void processControllers() {
        // 實作ItemAdapterRV類別，加入註冊監聽事件的工作
        itemAdapter = new ItemAdapterRV(items) {
            @Override
            public void onBindViewHolder(final ViewHolder holder, final int position) {
                super.onBindViewHolder(holder, position);

                // 建立與註冊項目點擊監聽物件
                holder.rootView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // 讀取選擇的記事物件
                        Item item = items.get(position);

                        // 如果已經有勾選的項目
                        if (selectedCount > 0) {
                            // 處理是否顯示已選擇項目
                            processMenu(item);
                            // 重新設定記事項目
                            items.set(position, item);
                        } else {
                            Intent intent = new Intent(
                                    "net.macdidi.myandroidtutorial.EDIT_ITEM");

                            // 設定記事編號與記事物件
                            intent.putExtra("position", position);
                            intent.putExtra("net.macdidi.myandroidtutorial.Item", item);

                            // 依照版本啟動Acvitity元件
                            startActivityForVersion(intent, 1);
                        }
                    }
                });

                // 建立與註冊項目長按監聽物件
                holder.rootView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        // 讀取選擇的記事物件
                        Item item = items.get(position);
                        // 處理是否顯示已選擇項目
                        processMenu(item);
                        // 重新設定記事項目
                        items.set(position, item);
                        return true;
                    }
                });
            }
        };

        // 設定RecylerView使用的資料來源
        item_list.setAdapter(itemAdapter);
    }

    // 處理是否顯示已選擇項目
    private void processMenu(Item item) {
        // 如果需要設定記事項目
        if (item != null) {
            // 設定已勾選的狀態
            item.setSelected(!item.isSelected());

            // 計算已勾選數量
            if (item.isSelected()) {
                selectedCount++;
            }
            else {
                selectedCount--;
            }
        }

        // 根據選擇的狀況，設定是否顯示選單項目
        add_item.setVisible(selectedCount == 0);
        search_item.setVisible(selectedCount == 0);
        revert_item.setVisible(selectedCount > 0);
        delete_item.setVisible(selectedCount > 0);

        // 通知項目勾選狀態改變
        itemAdapter.notifyDataSetChanged();
    }

    // 載入選單資源
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_main, menu);

        // 取得選單項目物件
        add_item = menu.findItem(R.id.add_item);
        search_item = menu.findItem(R.id.search_item);
        revert_item = menu.findItem(R.id.revert_item);
        delete_item = menu.findItem(R.id.delete_item);

        // 設定選單項目
        processMenu(null);

        return true;
    }

    // 使用者選擇所有的選單項目都會呼叫這個方法
    public void clickMenuItem(MenuItem item) {
        // 使用參數取得使用者選擇的選單項目元件編號
        int itemId = item.getItemId();

        // 判斷該執行什麼工作，目前還沒有加入需要執行的工作
        switch (itemId) {
            case R.id.search_item:
                break;
            // 使用者選擇新增選單項目
            case R.id.add_item:
                // 使用Action名稱建立啟動另一個Activity元件需要的Intent物件
                Intent intent = new Intent("net.macdidi.myandroidtutorial.ADD_ITEM");
                // 依照版本啟動Acvitity元件
                startActivityForVersion(intent, 0);
                break;
            // 取消所有已勾選的項目
            case R.id.revert_item:
                for (int i = 0; i < items.size(); i++) {
                    Item ri = items.get(i);

                    if (ri.isSelected()) {
                        ri.setSelected(false);
                        // 移除
                        //itemAdapter.set(i, ri);
                    }
                }

                selectedCount = 0;
                processMenu(null);

                break;
            // 刪除
            case R.id.delete_item:
                if (selectedCount == 0) {
                    break;
                }

                AlertDialog.Builder d = new AlertDialog.Builder(this);
                String message = getString(R.string.delete_item);
                d.setTitle(R.string.delete)
                        .setMessage(String.format(message, selectedCount));
                d.setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int index = items.size() - 1;

                                while (index > -1) {
                                    // 改為使用items物件
                                    Item item = items.get(index);

                                    if (item.isSelected()) {
                                        // 改為使用items物件
                                        items.remove(item);
                                        itemDAO.delete(item.getId());
                                    }

                                    index--;
                                }

                                // 移除
                                //itemAdapter.notifyDataSetChanged();
                                selectedCount = 0;
                                processMenu(null);
                            }
                        });
                d.setNegativeButton(android.R.string.no, null);
                d.show();

                break;
        }
    }

    // 點擊應用程式名稱元件後呼叫的方法
    public void aboutApp(View view) {
        // 建立啟動另一個Activity元件需要的Intent物件
        // 建構式的第一個參數：「this」
        // 建構式的第二個參數：「Activity元件類別名稱.class」
        Intent intent = new Intent(this, AboutActivity.class);
        // 呼叫「startActivity」，參數為一個建立好的Intent物件
        // 這行敘述執行以後，如果沒有任何錯誤，就會啟動指定的元件
        startActivity(intent);
    }

    // 設定
    public void clickPreferences(MenuItem item) {
        // 依照版本啟動Acvitity元件
        startActivityForVersion(new Intent(this, PrefActivity.class));
    }

    private void startActivityForVersion(Intent intent, int requestCode) {
        // 如果裝置的版本是LOLLIPOP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 加入畫面轉換設定
            startActivityForResult(intent, requestCode,
                    ActivityOptions.makeSceneTransitionAnimation(
                            MainActivity.this).toBundle());
        }
        else {
            startActivityForResult(intent, requestCode);
        }
    }

    private void startActivityForVersion(Intent intent) {
        // 如果裝置的版本是LOLLIPOP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 加入畫面轉換設定
            startActivity(intent,
                    ActivityOptions.makeSceneTransitionAnimation(
                            MainActivity.this).toBundle());
        }
        else {
            startActivity(intent);
        }
    }

}
