package com.cy8018.tvplayer.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.cy8018.tvplayer.R;
import com.cy8018.tvplayer.db.AppDatabase;
import com.cy8018.tvplayer.db.ChannelData;
import com.hbb20.CountryCodePicker;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

public class AddChannelDialog extends DialogFragment {

    private EditText channelName;
    private EditText channelURL;
    private EditText channelLogoURL;
    private EditText channelLanguage;
    private CountryCodePicker countryPicker;

    @NotNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_add_channel, null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                .setTitle("Add A New Channel")
                // Add action buttons
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        AddChannel();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        AddChannelDialog.this.getDialog().cancel();
                    }
                });

        channelName = view.findViewById(R.id.add_channel_name);
        channelURL = view.findViewById(R.id.add_channel_url);
        channelLogoURL = view.findViewById(R.id.add_channel_logo_url);
        channelLanguage = view.findViewById(R.id.add_channel_language);
        countryPicker = view.findViewById(R.id.add_channel_country);

        return builder.create();
    }

    private void AddChannel() {

        String channelNameStr = channelName.getText().toString();
        if (channelNameStr == null || channelNameStr.trim().length() == 0) {
            Toast toast = Toast.makeText(getContext(), "Please input Channel Name…", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        channelNameStr = channelNameStr.trim();

        if (AppDatabase.getInstance(getContext()).channelDao().isChannelExists(channelNameStr) > 0) {
            Toast toast = Toast.makeText(getContext(), "Channel Name exists, please use another name…", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }

        String channelUrlStr = channelURL.getText().toString();
        if (channelUrlStr == null || channelUrlStr.trim().length() == 0 || !URLUtil.isValidUrl(channelUrlStr.trim())) {
            Toast toast = Toast.makeText(getContext(), "Please input a correct Channel URL…", Toast.LENGTH_LONG);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
            return;
        }
        channelUrlStr = channelUrlStr.trim();

        String channelLogoUrlStr = channelLogoURL.getText().toString();
        if (channelLogoUrlStr != null && channelLogoUrlStr.trim().length() > 0) {
            channelLogoUrlStr = channelLogoUrlStr.trim();
            if (!URLUtil.isValidUrl(channelLogoUrlStr)) {
                Toast toast = Toast.makeText(getContext(), "Please input a correct Channel logo URL…", Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CENTER, 0, 0);
                toast.show();
                return;
            }
        }
        String channelLanguageStr = channelLanguage.getText().toString();
        if (channelLanguageStr != null) {
            channelLanguageStr = channelLanguageStr.trim();
        }
        String countryCode = countryPicker.getSelectedCountryNameCode();
        String countryName = countryPicker.getSelectedCountryName();

        ChannelData channel = new ChannelData();
        channel.name = channelNameStr;
        channel.url = new ArrayList<>();
        channel.url.add(channelUrlStr);
        channel.isFavorite = true;

        if (channelLogoUrlStr != null && channelLogoUrlStr.length() > 0) {
            channel.logo = channelLogoUrlStr;
        }
        if (channelLanguageStr != null && channelLanguageStr.length() > 0) {
            channel.languageName = channelLanguageStr;
        }
        if (countryCode != null && countryCode.length() > 0) {
            channel.countryCode = countryCode;
            channel.countryName = countryName;
        }

        AppDatabase.getInstance(getContext()).channelDao().insert(channel);
    }
}
