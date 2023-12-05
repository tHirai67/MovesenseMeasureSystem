package com.example.movesensemeasuresystem.activity_connection;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.movesensemeasuresystem.R;

import java.util.ArrayList;

public class MovesenseAdapter extends RecyclerView.Adapter<MovesenseAdapter.MovesenseViewHolder> {

    private ArrayList<MovesenseModel> movesenseList;

    private int batteryLevel=0;


    public static class MovesenseViewHolder extends RecyclerView.ViewHolder{
        private final TextView tvMacAddress;
        private final TextView tvSerial;
        private final TextView tvBattery;
        private CheckBox cbConnectCheck;

        public MovesenseViewHolder(@NonNull View itemView) {
            super(itemView);
            this.tvMacAddress = itemView.findViewById(R.id.tvMacAddress);
            this.tvSerial = itemView.findViewById(R.id.tvSerialNumber);
            this.tvBattery = itemView.findViewById(R.id.tvBattery);
            this.cbConnectCheck = itemView.findViewById(R.id.cbConnectCheck);
        }

        public TextView getTvMacAddress() {
            return tvMacAddress;
        }

        public TextView getTvSerial() {
            return tvSerial;
        }
    }

    public MovesenseAdapter(ArrayList<MovesenseModel> list){
        this.movesenseList = list;
    }

    @NonNull
    @Override
    public MovesenseAdapter.MovesenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_movesense, parent, false);
        MovesenseViewHolder holder = new MovesenseViewHolder(view);
        return holder;

    }

    @Override
    public void onBindViewHolder(@NonNull MovesenseViewHolder holder, @SuppressLint("RecyclerView") int position) {
        holder.getTvSerial().setText(movesenseList.get(position).getSerial());
        holder.getTvMacAddress().setText(movesenseList.get(position).getMacAddress());
        holder.tvBattery.setText(String.valueOf(batteryLevel)+"%");
        holder.cbConnectCheck.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.cbConnectCheck.isChecked()){
                    holder.cbConnectCheck.setChecked(true);
                    movesenseList.get(position).isConnect = true;
                }else {
                    holder.cbConnectCheck.setChecked(false);
                    movesenseList.get(position).isConnect = false;
                }
                Log.d("adapter", movesenseList.get(position).toString());

            }
        });
    }

    @Override
    public int getItemCount() {
        return movesenseList.size();
    }

    public void add(MovesenseModel movesense){
        if(!movesenseList.contains(movesense)){
            movesenseList.add(movesense);
        }
        notifyItemChanged(movesenseList.size());
    }

    public void setBattery(int batteryLevel){
        this.batteryLevel = batteryLevel;
    }
}
