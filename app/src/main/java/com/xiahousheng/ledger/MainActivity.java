package com.xiahousheng.ledger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private RecyclerView ledgerList;
    private TextView ledgerDay,ledgerWeek,ledgerMonth,ledgerTimeMonth;
    private FloatingActionButton addLedger;
    HorizonScrollViewAdapter adapter;
    public Handler handler;
    private static String UrlGetItem = "http://82.156.201.153/ledgerdb/getitems";
    private static String UrlUpload = "http://82.156.201.153/ledgerdb/upload";
    private static String UrlDelete = "http://82.156.201.153/ledgerdb/deleteitem";
    private static String UrlUpdate = "http://82.156.201.153/ledgerdb/update";
    private static String dateToday;
    private static OkHttpClient client = new OkHttpClient();
    private LedgerData dataDay,dataWeek,dataMonth;
    private LedgerPostRes uploadRes,deleteRes,updateRes;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        initHandler();
        ledgerList = findViewById(R.id.ledger_list);
        ledgerDay = findViewById(R.id.ledger_day_pay);
        ledgerWeek = findViewById(R.id.ledger_week_pay);
        ledgerMonth = findViewById(R.id.ledger_month_pay);
        ledgerTimeMonth = findViewById(R.id.ledger_time_month);
        addLedger = findViewById(R.id.ledger_button_add);
        requestInThread(0);
    }

    private void requestInThread(int what){
        new Thread(new Runnable() {
            @Override
            public void run() {
                LocalDate today = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                try {
                    dateToday = formatter.format(today);
                    dataMonth = getPayDataGson(2, dateToday);
                    //System.out.println("Month-ok");
                    dataWeek = getPayDataGson(1, dateToday);
                    //System.out.println("Week-ok");
                    dataDay = getPayDataGson(0, dateToday);
                    //System.out.println("Day-ok");
                }catch (IOException e){
                    System.out.println(e);
                }
                handler.sendMessage(handler.obtainMessage(what));
            }
        }).start();
    }

    private void upLoadInThread(int num,String type){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    uploadRes = uploadLedger(num,type);
                    handler.sendMessage(handler.obtainMessage(2));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void deleteInThread(int id){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    deleteRes = delete(id);
                    handler.sendMessage(handler.obtainMessage(3));
                }catch (IOException e){
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void updateInThread(int id,int num,String type){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    updateRes = update(id,num,type);
                    handler.sendMessage(handler.obtainMessage(5));
                }catch (IOException e){
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    private void initHandler(){
        handler = new Handler(Looper.getMainLooper()){
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void handleMessage(@NonNull Message msg) {
                //全初始化
                if(msg.what==0){
                    try {
                        initPay();
                        //System.out.println("what2|执行完毕");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                //全部刷新
                if(msg.what==1){
                    ledgerDay.setText(String.valueOf(dataDay.total));
                    ledgerWeek.setText(String.valueOf(dataWeek.total));
                    ledgerMonth.setText(String.valueOf(dataMonth.total));
                    adapter.clearData();
                    adapter.bindData(dataMonth.items);
                    adapter.notifyDataSetChanged();
                    ledgerList.smoothScrollToPosition(dataMonth.items.size() -1);
                }
                //单独刷新数据
                if(msg.what==4){
                    if (deleteRes.code == 1) {
                        ledgerDay.setText(String.valueOf(dataDay.total));
                        ledgerWeek.setText(String.valueOf(dataWeek.total));
                        ledgerMonth.setText(String.valueOf(dataMonth.total));
                    }
                }
                //上传
                if(msg.what==2){
                    if (uploadRes.code == 1) {
                        requestInThread(1);
                    }
                }
                //删除
                if(msg.what==3){
                    //重新请求数据
                    requestInThread(4);
                }
                //更新
                if(msg.what==5){
                    requestInThread(1);
                }
            }
        };
    }

    private void initPay() throws IOException {
        AlertDialog dialogAdd = initDialog();
        LinearLayoutManager manager = new LinearLayoutManager(this);
        initAdapter(dataMonth.items);
        ledgerList.setLayoutManager(manager);
        ledgerList.setAdapter(adapter);
        ledgerMonth.setText(String.valueOf(dataMonth.total));
        ledgerWeek.setText(String.valueOf(dataWeek.total));
        ledgerDay.setText(String.valueOf(dataDay.total));
        ledgerTimeMonth.setText(String.format("%s月支出",LocalDate.now().getMonthValue()));
        addLedger.setOnClickListener(view -> {
            dialogAdd.show();
        });
    }

    private AlertDialog initDialog(){
        AlertDialog.Builder dialogAdd = new AlertDialog.Builder(MainActivity.this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.ledger_add_dialog,null);

        if(dialogView.getParent() != null){
            ((ViewGroup) dialogView.getParent()).removeView(dialogView);
        }

        dialogAdd.setTitle("添加记录").setView(dialogView)
                .setPositiveButton("确认", (dialogInterface, i) -> {
                    RadioGroup group = dialogView.findViewById(R.id.ledger_select_group);
                    RadioButton button = dialogView.findViewById(group.getCheckedRadioButtonId());
                    EditText num = dialogView.findViewById(R.id.ledger_num_pay);
                    String payNum = num.getText().toString();
                    if (TextUtils.isEmpty(payNum)){
                        Toast.makeText(this,"不能花0块钱！",Toast.LENGTH_SHORT).show();
                    }
                    else {
                        upLoadInThread(Integer.parseInt(payNum), button.getText().toString());
                    }
                })
                .setNegativeButton("取消", ((dialogInterface, i) -> {
                    dialogInterface.dismiss();
                }));
        return  dialogAdd.create();
    }

    private LedgerPostRes uploadLedger(int num,String type) throws IOException {
        String json = String.format("{\"token\":\"%s\",\"num\":%s,\"type\":\"%s\"}",getUserToken(),num,type);
        return postWithJson(json,UrlUpload,LedgerPostRes.class);
    }

    private LedgerData getPayDataGson(int method,String date) throws IOException{
        String json = String.format("{\"token\":\"%s\",\"method\":%s,\"specificDate\":\"%s\"}",getUserToken(),method,date);
        return postWithJson(json,UrlGetItem,LedgerData.class);
    }

    private <T> T postWithJson(String json,String url,Class<T> clazz) throws IOException{
        Gson gson = new Gson();
        MediaType JSON = MediaType.get("application/json");
        RequestBody body = RequestBody.create(json,JSON);
        Request request = new Request.Builder()
                .post(body)
                .url(url)
                .build();
        Response response = client.newCall(request).execute();
        assert response.body() != null;
        return gson.fromJson(response.body().string(),clazz);
    }

    private String getUserToken() throws IOException{
        return readInternalString("token.txt");
    }

    private String readInternalString(String filePaN) throws IOException{
        File internalStorageDir = getFilesDir();
        File file = new File(internalStorageDir,filePaN);
        FileReader fileReader = new FileReader(file);
        StringBuilder content= new StringBuilder();
        int unicode;
        while((unicode = fileReader.read())!=-1){
            content.append((char) unicode);
        }
        fileReader.close();
        return content.toString();
    }

    private void initAdapter(List<LedgerItem> ledgerItems){
        adapter = new HorizonScrollViewAdapter();
        adapter.setOnItemDeleteListener(new HorizonScrollViewAdapter.OnItemDeleteListiner() {
            @Override
            public void onDelete(int id) {
                deleteInThread(id);
            }
        });
        adapter.setOnItemUpdateListener(new HorizonScrollViewAdapter.OnItemUpdateListiner() {
            @Override
            public void onUpdate(int id, int num,String type) { updateInThread(id,num,type);}
        });
        adapter.setLayout(R.layout.ledger_list_item_view);
        adapter.setContext(this);
        adapter.bind(ledgerList);
        adapter.bindData(ledgerItems);
    }

    private LedgerPostRes delete(int id) throws IOException{
        String json = String.format("{\"token\":\"%s\",\"id\":%s}",getUserToken(),id);
        return postWithJson(json,UrlDelete,LedgerPostRes.class);
    }

    private LedgerPostRes update(int id,int num,String type) throws IOException{
        String json = String.format("{\"token\":\"%s\",\"num\":%s,\"type\":\"%s\",\"id\":%s}",getUserToken(),num,type,id);
        return postWithJson(json,UrlUpdate,LedgerPostRes.class);
    }

    //适配器
    /*
    class LedgerListAdapter extends RecyclerView.Adapter<LedgerViewHolder>{
        private List<LedgerItem> ledgerItems;
        public void init(List<LedgerItem> items){
            this.ledgerItems = items;
        }

        @NonNull
        @Override
        public LedgerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.ledger_list_item_view,parent,false);
            return new LedgerViewHolder(view);
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        @Override
        public void onBindViewHolder(@NonNull LedgerViewHolder holder, int position) {
            LedgerItem item = ledgerItems.get(position);
             // 0 吃饭 1 娱乐 2 学习
            Drawable image = null;
            if(Objects.equals(item.type_, "吃饭")){
                image=getApplicationContext().getDrawable(R.drawable.baseline_set_meal_24);
            }
            if(Objects.equals(item.type_, "学习")){
                image=getApplicationContext().getDrawable(R.drawable.baseline_menu_book_24);
            }
            if(Objects.equals(item.type_, "娱乐")){
                image=getApplicationContext().getDrawable(R.drawable.baseline_videogame_asset_24);
            }
            //测试
            if(Objects.equals(item.type_,"测试")){
                image=getApplicationContext().getDrawable(R.drawable.baseline_videogame_asset_24);
            }
            holder.ledgerImageType.setImageDrawable(image);
            holder.ledgerTime.setText(String.valueOf(item.time_));
            holder.ledgerPayNum.setText(String.valueOf(item.num));
        }

        @Override
        public int getItemCount() {
            return ledgerItems.size();
        }
    }
    */


    //item的视图
    /*
    static class LedgerViewHolder extends  HorizonScrollViewHolder{
        ImageView ledgerImageType;
        TextView ledgerTime;
        TextView ledgerPayNum;

        public LedgerViewHolder(View itemView){
            super(itemView);
            ledgerImageType = itemView.findViewById(R.id.ledger_item_image_type);
            ledgerTime = itemView.findViewById(R.id.ledger_item_paytime);
            ledgerPayNum = itemView.findViewById(R.id.ledger_item_num);
        }
    }
    */

    //返回数据对象
    static class LedgerData{
        List<LedgerItem> items;
        int total;
        int code;
    }

    static class LedgerItem{
        int num;
        String type_;
        String time_;
        int id;
    }

    static class LedgerPostRes{
        int code;
        String msg;
    }


}