package com.cy8018.tvplayer.ui;


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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cy8018.tvplayer.R;
import com.cy8018.tvplayer.db.AppDatabase;
import com.cy8018.tvplayer.db.ChannelData;

import java.util.ArrayList;
import java.util.List;

public class ChannelListAdapter extends RecyclerView.Adapter<ChannelListAdapter.ViewHolder> implements Filterable {

    private static final String TAG = "StationListAdapter";

    private int selectedPos = RecyclerView.NO_POSITION;

    private String selectedName = "";

    private List<ChannelData> mChannelList;
    private List<ChannelData> mChannelListFull;
    private Context mContext;

    ChannelListAdapter(Context context, List<ChannelData> channelList) {
        this.mContext = context;
        this.mChannelList = channelList;
        if (mChannelList != null) {
            mChannelListFull = new ArrayList<>(mChannelList);
        }
    }

    ChannelListAdapter(Context context, List<ChannelData> channelList, int selectedPos) {
        this.mContext = context;
        this.mChannelList = channelList;
        if (mChannelList != null) {
            mChannelListFull = new ArrayList<>(mChannelList);
        }
        this.selectedPos = selectedPos;
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

        String logoUrl = mChannelList.get(position).logo;

        if (logoUrl == null || logoUrl.isEmpty()) {
            holder.stationLogo.setImageResource(mContext.getResources().getIdentifier("@drawable/tv_transparent", null, mContext.getPackageName()));
        }
        else {
            if (!logoUrl.toLowerCase().contains("http")) {
                logoUrl = MainActivity.CurrentServerPrefix + "logo/" + logoUrl;
            }
            // Load the station logo.
            Glide.with(mContext)
                    .asBitmap()
                    .timeout(15000)
                    .load(logoUrl)
                    .into(holder.stationLogo);
        }

        if (mChannelList.get(position).countryCode != null && mChannelList.get(position).countryCode.length() > 0) {
            holder.flag.setImageResource(mContext.getResources().getIdentifier("@drawable/"+ mChannelList.get(position).countryCode, null, mContext.getPackageName()));
        }

        // Set the station title
        holder.stationTitle.setText(mChannelList.get(position).name);
        if (mChannelList.get(position).isFavorite) {
            holder.isFavoriteIcon.setImageResource(mContext.getResources().getIdentifier("@drawable/ic_star", null, mContext.getPackageName()));
        }
        else {
            holder.isFavoriteIcon.setImageResource(mContext.getResources().getIdentifier("@drawable/ic_star_outline", null, mContext.getPackageName()));
        }

        holder.isFavoriteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChannelList.get(position).isFavorite) {
                    ((MainActivity)mContext).removeFromFavorites(mChannelList.get(position).name);
                    holder.isFavoriteIcon.setImageResource(mContext.getResources().getIdentifier("@drawable/ic_star_outline", null, mContext.getPackageName()));
                } else {
                    ((MainActivity)mContext).addToFavorites(mChannelList.get(position).name);
                    holder.isFavoriteIcon.setImageResource(mContext.getResources().getIdentifier("@drawable/ic_star", null, mContext.getPackageName()));
                }
            }
        });

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
                msg.obj = mChannelList.get(selectedPos);
                msg.what = MainActivity.MSG_PLAY;
                ((MainActivity)mContext).mHandler.sendMessage(msg);

                getFilter().filter("");

                Log.d(TAG, "onClick: clicked on:" + mChannelList.get(selectedPos).name);
            }
        });

        holder.itemView.setSelected(selectedPos == position);
    }

    @Override
    public int getItemCount() {
        if (mChannelList == null) {
            return 0;
        }
        return mChannelList.size();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }

    private Filter filter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<ChannelData> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(mChannelListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (ChannelData item : mChannelListFull) {
                    if (
                            (item.name != null && item.name.toLowerCase().contains(filterPattern.toLowerCase()))
                            || (item.category != null && item.category.toLowerCase().contains(filterPattern.toLowerCase()))
                            || (item.languageName != null && item.languageName.toLowerCase().contains(filterPattern.toLowerCase()))
                            || (item.countryName != null && item.countryName.toLowerCase().contains(filterPattern.toLowerCase()))
                            || (item.countryCode != null && item.countryCode.toLowerCase().contains(filterPattern.toLowerCase()))
                    ) {
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
            mChannelList.clear();
            if (results.values != null)
            {
                mChannelList.addAll((List) results.values);
            }

            if (selectedPos >= 0)
            {
                selectedPos = mChannelList.indexOf(mChannelListFull.get(selectedPos));
                notifyItemChanged(selectedPos);
            }

            notifyDataSetChanged();
        }
    };

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView stationTitle;
        ImageView stationLogo, isFavoriteIcon, flag;
        RelativeLayout parentLayout;

        private ViewHolder (View itemView) {
            super(itemView);
            stationTitle = itemView.findViewById(R.id.stationTitle);
            stationLogo = itemView.findViewById(R.id.logo);
            flag = itemView.findViewById(R.id.station_country_flag);
            isFavoriteIcon = itemView.findViewById(R.id.station_favorite_icon);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}
