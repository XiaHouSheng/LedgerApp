package com.xiahousheng.ledger;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
    LedgerListAdapter adapter;
    private static String UrlGetItem = "http://82.156.201.153/ledgerdb/getitems";
    private static String UrlUpload = "http://82.156.201.153/ledgerdb/upload";
    private static String dateToday;
    private static OkHttpClient client = new OkHttpClient();
    private LedgerData dataDay,dataWeek,dataMonth;
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
        ledgerList = findViewById(R.id.ledger_list);
        ledgerDay = findViewById(R.id.ledger_day_pay);
        ledgerWeek = findViewById(R.id.ledger_week_pay);
        ledgerMonth = findViewById(R.id.ledger_month_pay);
        ledgerTimeMonth = findViewById(R.id.ledger_time_month);
        addLedger = findViewById(R.id.ledger_button_add);
        try {
            initPay();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initPay() throws IOException {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        dateToday = formatter.format(today);
        dataMonth = getPayDataGson(2,dateToday);
        //System.out.println("Month-ok");
        dataWeek = getPayDataGson(1,dateToday);
        //System.out.println("Week-ok");
        dataDay = getPayDataGson(0,dateToday);
        //System.out.println("Day-ok");
        AlertDialog dialogAdd = initDialog();


        adapter = new LedgerListAdapter();
        LinearLayoutManager manager = new LinearLayoutManager(this);
        adapter.init(dataMonth.items);
        ledgerList.setLayoutManager(manager);
        ledgerList.setAdapter(adapter);
        ledgerMonth.setText(String.valueOf(dataMonth.total));
        ledgerWeek.setText(String.valueOf(dataWeek.total));
        ledgerDay.setText(String.valueOf(dataDay.total));
        ledgerTimeMonth.setText(String.format("%s月支出",today.getMonthValue()));
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
                        try {
                            LedgerUploadRes response = uploadLedger(Integer.parseInt(payNum), button.getText().toString());
                            if (response.code == 1) {
                                refresh();
                            }
                            Toast.makeText(this,response.msg,Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
                .setNegativeButton("取消", ((dialogInterface, i) -> {
                    dialogInterface.dismiss();
                }));
        return  dialogAdd.create();
    }

    private LedgerUploadRes uploadLedger(int num,String type) throws IOException {
        String json = String.format("{\"token\":\"%s\",\"num\":%s,\"type\":\"%s\"}",getUserToken(),num,type);
        return postWithJson(json,UrlUpload,LedgerUploadRes.class);
    }

    private LedgerData getPayDataGson(int method,String date) throws IOException{
        String json = String.format("{\"token\":\"%s\",\"method\":%s,\"specificDate\":\"%s\"}",getUserToken(),method,date);
        return postWithJson(json,UrlGetItem,LedgerData.class);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void refresh() throws IOException{
        dataDay = getPayDataGson(0,dateToday);ledgerDay.setText(String.valueOf(dataDay.total));
        dataWeek = getPayDataGson(1,dateToday);ledgerWeek.setText(String.valueOf(dataWeek.total));
        dataMonth = getPayDataGson(2,dateToday);ledgerMonth.setText(String.valueOf(dataMonth.total));
        adapter.init(dataMonth.items);
        adapter.notifyDataSetChanged();//庞大数据下性能会出现问题|后面再想办法
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

    //适配器
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

    //item的视图
    static class LedgerViewHolder extends RecyclerView.ViewHolder{
        ImageView ledgerImageType;
        TextView ledgerTime;
        TextView ledgerPayNum;

        public LedgerViewHolder(View itemView){
            super(itemView);
            itemView.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            ledgerImageType = itemView.findViewById(R.id.ledger_item_image_type);
            ledgerTime = itemView.findViewById(R.id.ledger_item_paytime);
            ledgerPayNum = itemView.findViewById(R.id.ledger_item_num);
        }
    }

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
    }

    static class LedgerUploadRes{
        int code;
        String msg;
    }

}