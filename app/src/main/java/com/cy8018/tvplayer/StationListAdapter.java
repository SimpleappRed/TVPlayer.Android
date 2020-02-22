package com.cy8018.tvplayer;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;


import java.util.List;

public class StationListAdapter extends RecyclerView.Adapter<StationListAdapter.ViewHolder>{

    private static final String TAG = "StationListAdapter";

    private int selectedPos = RecyclerView.NO_POSITION;

    private List<Station> mStationList;
    private Context mContext;

    StationListAdapter(Context context, List<Station> stationList) {
        this.mContext = context;
        this.mStationList = stationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {
        Log.d(TAG, "onBindViewHolder: called.");

        // Set the station title
        holder.stationTitle.setText(mStationList.get(position).name);

        // Set OnClickListener
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: clicked on:" + mStationList.get(position).name);

                // Below line is just like a safety check, because sometimes holder could be null,
                // in that case, getAdapterPosition() will return RecyclerView.NO_POSITION
                if (holder.getAdapterPosition() == RecyclerView.NO_POSITION) return;

                // Updating old as well as new positions
                notifyItemChanged(selectedPos);
                selectedPos = holder.getAdapterPosition();
                notifyItemChanged(selectedPos);


                // Send MSG_PLAY message to main thread to play the radio
                Message msg = new Message();
                msg.obj = position;
                msg.what = MainActivity.MSG_PLAY;
                ((MainActivity)mContext).mHandler.sendMessage(msg);
            }
        });

        holder.itemView.setSelected(selectedPos == position);
    }

    @Override
    public int getItemCount() {
        if (mStationList == null)
        {
            return 0;
        }
        return mStationList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView stationTitle;
        RelativeLayout parentLayout;

        private ViewHolder (View itemView)
        {
            super(itemView);
            stationTitle = itemView.findViewById(R.id.stationTitle);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}
