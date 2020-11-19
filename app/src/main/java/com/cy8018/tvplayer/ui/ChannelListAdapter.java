package com.cy8018.tvplayer.ui;


import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.os.Build;
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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cy8018.tvplayer.R;
import com.cy8018.tvplayer.db.ChannelData;
import com.cy8018.tvplayer.util.SvgSoftwareLayerSetter;

import java.util.ArrayList;
import java.util.List;

public class ChannelListAdapter extends RecyclerView.Adapter<ChannelListAdapter.ViewHolder> implements Filterable {

    private static final String TAG = "StationListAdapter";

    private int selectedPos = RecyclerView.NO_POSITION;

    private final String selectedName = "";

    private final List<ChannelData> mChannelList;
    private List<ChannelData> mChannelListFull;
    private final Context mContext;

    private RequestBuilder<PictureDrawable> requestBuilder;

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
            holder.stationLogo.setImageResource(R.drawable.tv_logo_trans);
        }
        else {
            // Load the station logo.
            Glide.with(mContext)
                    .asBitmap()
                    .timeout(3000)
                    .placeholder(R.drawable.tv_logo_trans)
                    .error(R.drawable.tv_logo_trans)
                    .load(logoUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.stationLogo);
        }

        if (mChannelList.get(position).countryCode != null && mChannelList.get(position).countryCode.length() > 0) {
            String flagUrl = ((MainActivity)mContext).getFlagResourceByCountry(mChannelList.get(position).countryCode.toLowerCase());
            if (flagUrl != null) {
                requestBuilder =
                        Glide.with(mContext)
                                .as(PictureDrawable.class)
                                //.placeholder(R.drawable.globe)
                                .error(R.drawable.globe)
                                .listener(new SvgSoftwareLayerSetter());

                requestBuilder.load(Uri.parse(flagUrl)).into(holder.flag);
            }
        }
        else {
            holder.flag.setImageResource(R.drawable.globe);
        }

        // Set the station title
        holder.stationTitle.setText(mChannelList.get(position).name);
        if (mChannelList.get(position).isFavorite) {
            holder.isFavoriteIcon.setImageResource(R.drawable.star);
        }
        else {
            holder.isFavoriteIcon.setImageResource(R.drawable.star_outline);
        }

        holder.isFavoriteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mChannelList.get(position).isFavorite) {
                    ((MainActivity)mContext).removeFromFavorites(mChannelList.get(position).name);
                    holder.isFavoriteIcon.setImageResource(R.drawable.star_outline);
                } else {
                    ((MainActivity)mContext).addToFavorites(mChannelList.get(position).name);
                    holder.isFavoriteIcon.setImageResource(R.drawable.star);
                }
            }
        });

        String desc = "";

        if (mChannelList.get(position).countryName != null && mChannelList.get(position).countryName.trim().length() > 0) {
            desc += mChannelList.get(position).countryName.trim();
        }
        if (mChannelList.get(position).languageName != null && mChannelList.get(position).languageName.trim().length() > 0) {
            if (desc.length() > 0) {
                desc += ", ";
            }
            desc += mChannelList.get(position).languageName.trim();
        }
        if (mChannelList.get(position).category != null && mChannelList.get(position).category.trim().length() > 0) {
            if (desc.length() > 0) {
                desc += ", ";
            }
            desc += mChannelList.get(position).category.trim();
        }

        if (desc.length() > 0) {
            holder.description.setText(desc);
        }

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

                //getFilter().filter("");

                Log.d(TAG, "onClick: clicked on:" + mChannelList.get(selectedPos).name);
            }
        });

        holder.itemView.setSelected(selectedPos == position);

        if (holder.itemView.isSelected()) {
            holder.listItemBar.setBackgroundResource(R.drawable.card_view_bg_selected);
        }
        else  {
            holder.listItemBar.setBackgroundResource(R.drawable.card_view_bg);
        }
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

    private final Filter filter = new Filter() {
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

        TextView stationTitle, description;
        ImageView stationLogo, isFavoriteIcon, flag;
        RelativeLayout parentLayout;
        CardView listItemBar;

        private ViewHolder (View itemView) {
            super(itemView);
            stationTitle = itemView.findViewById(R.id.channel_name_list_item);
            description = itemView.findViewById(R.id.channel_description_list_item);
            stationLogo = itemView.findViewById(R.id.channel_logo_list_item);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stationLogo.setClipToOutline(true);
            }
            flag = itemView.findViewById(R.id.country_flag_list_item);
            isFavoriteIcon = itemView.findViewById(R.id.fav_icon_list_item);
            listItemBar = itemView.findViewById(R.id.list_item_bar);
            parentLayout = itemView.findViewById(R.id.parent_layout);
        }
    }
}
