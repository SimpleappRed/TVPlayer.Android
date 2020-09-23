package com.cy8018.tvplayer.ui;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.cy8018.tvplayer.R;
import com.cy8018.tvplayer.model.Station;

import java.util.ArrayList;
import java.util.List;

public class StationListAdapter extends RecyclerView.Adapter<StationListAdapter.ViewHolder> implements Filterable {

    private static final String TAG = "StationListAdapter";

    private int selectedPos = RecyclerView.NO_POSITION;

    private List<Station> mStationList;
    private List<Station> mStationListFull;
    private Context mContext;

    StationListAdapter(Context context, List<Station> stationList) {
        this.mContext = context;
        this.mStationList = stationList;
        mStationListFull = new ArrayList<>(mStationList);
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

        String logoUrl = mStationList.get(position).logo;

        if (logoUrl == null || logoUrl.isEmpty()) {
            holder.stationLogo.setImageResource(mContext.getResources().getIdentifier("@drawable/tv", null, mContext.getPackageName()));
        }
        else {
            if (!logoUrl.toLowerCase().contains("http")) {
                logoUrl = MainActivity.CurrentServerPrefix + "logo/" + logoUrl;
            }
            // Load the station logo.
            Glide.with(mContext)
                    .asBitmap()
                    .timeout(10000)
                    .load(logoUrl)
                    .into(holder.stationLogo);
        }

        // Set the station title
        holder.stationTitle.setText(mStationList.get(position).name);

        // Set OnClickListener
        holder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                // Below line is just like a safety check, because sometimes holder could be null,
                // in that case, getAdapterPosition() will return RecyclerView.NO_POSITION
                if (holder.getAdapterPosition() == RecyclerView.NO_POSITION) return;

                // Updating old as well as new positions
                notifyItemChanged(selectedPos);
                selectedPos = holder.getAdapterPosition();
                notifyItemChanged(selectedPos);

                // Send MSG_PLAY message to main thread to play the radio
                Message msg = new Message();
                msg.obj = selectedPos;
                msg.what = MainActivity.MSG_PLAY;
                ((MainActivity)mContext).mHandler.sendMessage(msg);

                Log.d(TAG, "onClick: clicked on:" + mStationList.get(selectedPos).name);
            }
        });

        holder.itemView.setSelected(selectedPos == position);
    }

    @Override
    public int getItemCount() {
        if (mStationList == null) {
            return 0;
        }
        return mStationList.size();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Station> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(mStationListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Station item : mStationListFull) {
                    if (item.name.toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mStationList.clear();
            mStationList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView stationTitle;
        ImageView stationLogo;
        RelativeLayout parentLayout;

        private ViewHolder (View itemView) {
            super(itemView);
            stationTitle = itemView.findViewById(R.id.stationTitle);
            stationLogo = itemView.findViewById(R.id.logo);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}