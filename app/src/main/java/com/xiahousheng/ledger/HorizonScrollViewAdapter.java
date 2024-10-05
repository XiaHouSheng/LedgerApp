package com.xiahousheng.ledger;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.xiahousheng.ledger.MainActivity.LedgerItem;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Objects;

public  class HorizonScrollViewAdapter extends RecyclerView.Adapter<HorizonScrollViewHolder> {
    private Context context;
    private int layout;
    private RecyclerView recyclerView;
    private List<LedgerItem> ledgerItems;
    private OnItemDeleteListiner deleteListiner;
    private OnItemUpdateListiner updateListiner;

    public void setContext (Context context){
        this.context = context;
    }

    public void setLayout(int layout){
        this.layout = layout;
    }

    public void bind(RecyclerView recyclerView){
        this.recyclerView=recyclerView;
    }

    public void bindData(List<LedgerItem> ledgerItems){
        this.ledgerItems = ledgerItems;
    }

    public void clearData(){
        ledgerItems.clear();
    }

    public interface OnItemDeleteListiner{
        void onDelete(int id);
    }

    public interface OnItemUpdateListiner{
        void onUpdate(int id,int num,String type);
    }

    public void setOnItemDeleteListener(OnItemDeleteListiner listiner){
        this.deleteListiner = listiner;
    }

    public void setOnItemUpdateListener(OnItemUpdateListiner listiner){
        this.updateListiner = listiner;
    }

    @NonNull
    @Override
    public HorizonScrollViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(layout,parent,false);
        return new HorizonScrollViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HorizonScrollViewHolder holder, int position) {

        LinearLayout linearLayout = holder.itemView.findViewById(R.id.containerDelete);

        if (linearLayout.getChildCount()==0) {
            ImageView imageViewDelete = new ImageView(context);
            imageViewDelete.setImageResource(R.drawable.baseline_delete_24);
            setParamsWidth(linearLayout,0);
            linearLayout.addView(imageViewDelete);

            ImageView imageViewUpdate = new ImageView(context);
            imageViewUpdate.setImageResource(R.drawable.baseline_edit_24);
            setParamsWidth(linearLayout,0);
            linearLayout.addView(imageViewUpdate);
        }

        if (linearLayout.getWidth() != 0){
            setParamsWidth(linearLayout,0);
        }

        linearLayout.getChildAt(0).setOnClickListener(view -> deleteDialog(holder.getAdapterPosition()).show());
        linearLayout.getChildAt(1).setOnClickListener(view -> updateDialog(holder.getAdapterPosition()).show());

        LedgerItem item = ledgerItems.get(position);
        // 0 吃饭 1 娱乐 2 学习
        Drawable image = null;
        if(Objects.equals(item.type_, "吃饭")){
            image=context.getDrawable(R.drawable.baseline_set_meal_24);
        }
        if(Objects.equals(item.type_, "学习")){
            image=context.getDrawable(R.drawable.baseline_menu_book_24);
        }
        if(Objects.equals(item.type_, "娱乐")){
            image=context.getDrawable(R.drawable.baseline_videogame_asset_24);
        }
        //测试
        if(Objects.equals(item.type_,"测试")){
            image=context.getDrawable(R.drawable.baseline_videogame_asset_24);
        }
        holder.ledgerImageType.setImageDrawable(image);
        holder.ledgerTime.setText(String.valueOf(item.time_));
        holder.ledgerPayNum.setText(String.valueOf(item.num));

        holder.itemView.setOnTouchListener(new View.OnTouchListener() {
            final View deleteContainer = holder.itemView.findViewById(R.id.containerDelete);
            ValueAnimator animator;
            float startX;
            float endX;
            float startY;
            final int maxWidth = 300;
            final int restrictWidth = 100;
            final int defaultWidth = 0;
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    startX = motionEvent.getX();
                    startY = motionEvent.getY();
                    recyclerView.requestDisallowInterceptTouchEvent(true);
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_UP){
                    endX =  motionEvent.getX();
                    if (startX - endX <0){
                        animator = ValueAnimator.ofInt(deleteContainer.getWidth(),defaultWidth);
                        animator.setDuration(1000);
                        animator.addUpdateListener(animation -> {
                            int value = (int) animation.getAnimatedValue();
                            setParamsWidth(deleteContainer,value);
                        });
                        animator.start();
                    }else{
                        if (deleteContainer.getWidth() > (maxWidth - 150) ){
                            animator = ValueAnimator.ofInt(deleteContainer.getWidth(),maxWidth);
                            animator.setDuration(1000);
                            animator.addUpdateListener(animation -> {
                                int value = (int) animation.getAnimatedValue();
                                setParamsWidth(deleteContainer,value);
                            });
                            animator.start();
                            //deleteContainer.setWidth(maxWidth);
                        } else {
                            animator = ValueAnimator.ofInt(deleteContainer.getWidth(),defaultWidth);
                            animator.setDuration(1000);
                            animator.addUpdateListener(animation -> {
                                int value = (int) animation.getAnimatedValue();
                                setParamsWidth(deleteContainer,value);
                            });
                            animator.start();
                            //deleteContainer.setWidth(defaultWidth);
                        }
                    }
                    recyclerView.requestDisallowInterceptTouchEvent(false);
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_MOVE){
                    int Abswidth = (int)  Math.abs( startX - motionEvent.getX());
                    int Absheight = (int) Math.abs( startY - motionEvent.getY());
                    int width = (int)  (startX - motionEvent.getX());
                    if (Abswidth > Absheight) {
                        if (deleteContainer.getWidth() <= maxWidth && width >=0 &&width - restrictWidth > 0) {
                            setParamsWidth(deleteContainer,width - restrictWidth);
                            System.out.println(width - restrictWidth);
                        }
                    }else {
                        animator = ValueAnimator.ofInt(deleteContainer.getWidth(),defaultWidth);
                        animator.setDuration(1000);
                        animator.addUpdateListener(animation -> {
                            int value = (int) animation.getAnimatedValue();
                            setParamsWidth(deleteContainer,value);
                        });
                        animator.start();
                        recyclerView.requestDisallowInterceptTouchEvent(false);
                    }
                }
                return true;
            }
        });
    }

    private void setParamsWidth(View view,int width){
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        view.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return ledgerItems.size();
    }

    private Dialog deleteDialog(int position){
        AlertDialog dialog = new AlertDialog.Builder(this.context)
                .setTitle("是否删除")
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(deleteListiner != null){
                            deleteListiner.onDelete(ledgerItems.get(position).id);
                        }
                        ledgerItems.remove(position);
                        notifyItemRemoved(position);

                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).create();
        return dialog;
    }

    private Dialog updateDialog(int position){
        View dialogView = LayoutInflater.from(context).inflate(R.layout.ledger_update_dialog,null);
        EditText numPay = dialogView.findViewById(R.id.ledger_num_pay_update);
        numPay.setText(String.valueOf(ledgerItems.get(position).num));

        AlertDialog dialogUpdate = new AlertDialog.Builder(this.context)
                .setTitle("修改记录").setView(dialogView)
                .setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String payNum = numPay.getText().toString();
                        if (TextUtils.isEmpty(payNum)){
                            Toast.makeText(context,"不能花0块钱！",Toast.LENGTH_SHORT).show();
                        }
                        else {
                            if(updateListiner != null){
                                updateListiner.onUpdate(ledgerItems.get(position).id,Integer.parseInt(payNum),ledgerItems.get(position).type_);
                            }
                        }
                    }
                })
                .setNegativeButton("取消", ((dialogInterface, i) -> {
                    dialogInterface.dismiss();
                })).create();
        return  dialogUpdate;
    }

}

class HorizonScrollViewHolder extends RecyclerView.ViewHolder{

    ImageView ledgerImageType;
    TextView ledgerTime;
    TextView ledgerPayNum;

    public HorizonScrollViewHolder(View itemView){
        super(itemView);
        ledgerImageType = itemView.findViewById(R.id.ledger_item_image_type);
        ledgerTime = itemView.findViewById(R.id.ledger_item_paytime);
        ledgerPayNum = itemView.findViewById(R.id.ledger_item_num);
    }

}




