package com.cy8018.tvplayer.util;

import android.icu.lang.UProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple Parser for M3U playlists
 */
@SuppressWarnings({"WeakerAccess", "CaughtExceptionImmediatelyRethrown", "unused", "SpellCheckingInspection", "TryFinallyCanBeTryWithResources"})
public class SimpleM3UParser {
    private final static String EXTINF_TAG = "#EXTINF:";
    private final static String EXTINF_TVG_NAME = "tvg-name=\"";
    private final static String EXTINF_TVG_ID = "tvg-id=\"";
    private final static String EXTINF_TVG_LOGO = "tvg-logo=\"";
    private final static String EXTINF_TVG_EPGURL = "tvg-epgurl=\"";
    private final static String EXTINF_TVG_URL = "tvg-url=\"";
    private final static String EXTINF_TVG_LANGUAGE = "tvg-language=\"";
    private final static String EXTINF_TVG_COUNTRY = "tvg-country=\"";
    private final static String EXTINF_GROUP_TITLE = "group-title=\"";
    private final static String EXTINF_RADIO = "radio=\"";
    private final static String EXTINF_TAGS = "tags=\"";

    // ########################
    // ##
    // ## Members
    // ##
    // ########################
    private ArrayList<M3U_Entry> _entries;
    private M3U_Entry _lastEntry;

    // Parse m3u file by reading content from file by filepath
    public ArrayList<M3U_Entry> parse(String filepath) throws IOException {
        return parse(new FileInputStream(filepath));
    }

    public ArrayList<M3U_Entry> parseM3UString(String str) throws IOException {
        _entries = new ArrayList<>();
        BufferedReader br = null;
        String line;

        try {
            br = new BufferedReader(new StringReader(str));
            while ((line = br.readLine()) != null) {
                try {
                    parseLine(line);
                } catch (Exception e) {
                    _lastEntry = null;
                }
            }
        } catch (IOException rethrow) {
            throw rethrow;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return _entries;
    }

    // Parse m3u file by reading from inputstream
    public ArrayList<M3U_Entry> parse(InputStream inputStream) throws IOException {
        _entries = new ArrayList<>();
        BufferedReader br = null;
        String line;

        try {
            br = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = br.readLine()) != null) {
                try {
                    parseLine(line);
                } catch (Exception e) {
                    _lastEntry = null;
                }
            }
        } catch (IOException rethrow) {
            throw rethrow;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
        return _entries;
    }

    // Parse one line of m3u
    private void parseLine(String line) {
        line = line.trim();

        // EXTINF line
        if (line.startsWith(EXTINF_TAG)) {
            _lastEntry = parseExtInf(line);
        }
        // URL line (no comment, no empty line(trimmed))
        else if (!line.isEmpty() && !line.startsWith("#")) {
            if (_lastEntry == null) {
                _lastEntry = new M3U_Entry();
            }
            _lastEntry.setUrl(line);
            _entries.add(_lastEntry);
            _lastEntry = null;
        }
        // No useable data -> reset last EXTINF for next entry
        else {
            _lastEntry = null;
        }
    }

    private M3U_Entry parseExtInf(String line) {
        M3U_Entry curEntry = new M3U_Entry();
        StringBuilder buf = new StringBuilder(20);
        if (line.length() < EXTINF_TAG.length() + 1) {
            return curEntry;
        }

        // Strip tag
        line = line.substring(EXTINF_TAG.length());

        // Read seconds (may end with comma or whitespace)
        while (line.length() > 0) {
            char c = line.charAt(0);
            if (Character.isDigit(c) || c == '-' || c == '+') {
                buf.append(c);
                line = line.substring(1);
            } else {
                break;
            }
        }
        if (buf.length() == 0 || line.isEmpty()) {
            return curEntry;
        }
        curEntry.setSeconds(Integer.valueOf(buf.toString()));

        // tvg tags
        while (!line.isEmpty() && !line.startsWith(",")) {
            line = line.trim();
            if (line.startsWith(EXTINF_TVG_NAME) && line.length() > EXTINF_TVG_NAME.length()) {
                line = line.substring(EXTINF_TVG_NAME.length());
                int i = line.indexOf("\"");
                curEntry.setTvgName(line.substring(0, i));
                line = line.substring(i + 1);
            }
            else if (line.startsWith(EXTINF_TVG_LOGO) && line.length() > EXTINF_TVG_LOGO.length()) {
                line = line.substring(EXTINF_TVG_LOGO.length());
                int i = line.indexOf("\"");
                curEntry.setTvgLogo(line.substring(0, i));
                line = line.substring(i + 1);
            }
            else if (line.startsWith(EXTINF_TVG_EPGURL) && line.length() > EXTINF_TVG_EPGURL.length()) {
                line = line.substring(EXTINF_TVG_EPGURL.length());
                int i = line.indexOf("\"");
                curEntry.setTvgEpgUrl(line.substring(0, i));
                line = line.substring(i + 1);
            }
            else if (line.startsWith(EXTINF_TVG_URL) && line.length() > EXTINF_TVG_URL.length()) {
                line = line.substring(EXTINF_TVG_URL.length());
                int i = line.indexOf("\"");
                curEntry.setTvgUrl(line.substring(0, i));
                line = line.substring(i + 1);
            }
            else if (line.startsWith(EXTINF_RADIO) && line.length() > EXTINF_RADIO.length()) {
                line = line.substring(EXTINF_RADIO.length());
                int i = line.indexOf("\"");
                curEntry.setIsRadio(Boolean.parseBoolean(line.substring(0, i)));
                line = line.substring(i + 1);
            }
            else if (line.startsWith(EXTINF_GROUP_TITLE) && line.length() > EXTINF_GROUP_TITLE.length()) {
                line = line.substring(EXTINF_GROUP_TITLE.length());
                int i = line.indexOf("\"");
                curEntry.setGroupTitle(line.substring(0, i));
                line = line.substring(i + 1);
            }
            else if (line.startsWith(EXTINF_TVG_ID) && line.length() > EXTINF_TVG_ID.length()) {
                line = line.substring(EXTINF_TVG_ID.length());
                int i = line.indexOf("\"");
                curEntry.setTvgId(line.substring(0, i));
                line = line.substring(i + 1);
            }
            else if (line.startsWith(EXTINF_TVG_LANGUAGE) && line.length() > EXTINF_TVG_LANGUAGE.length()) {
                line = line.substring(EXTINF_TVG_LANGUAGE.length());
                int i = line.indexOf("\"");
                curEntry.setTvgLanguage(line.substring(0, i));
                line = line.substring(i + 1);
            }
            else if (line.startsWith(EXTINF_TVG_COUNTRY) && line.length() > EXTINF_TVG_COUNTRY.length()) {
                line = line.substring(EXTINF_TVG_COUNTRY.length());
                int i = line.indexOf("\"");
                curEntry.setTvgCountry(line.substring(0, i));
                line = line.substring(i + 1);
            }
            else if (line.startsWith(EXTINF_TAGS) && line.length() > EXTINF_TAGS.length()) {
                line = line.substring(EXTINF_TAGS.length());
                int i = line.indexOf("\"");
                curEntry.setTags(line.substring(0, i).split(","));
                line = line.substring(i + 1);
            }
            else
            {
                int i = line.indexOf("\"");
                line = line.substring(i + 1);
            }
        }

        // Name
        line = line.trim();
        if (line.length() > 1 && line.startsWith(",")) {
            line = line.substring(1);
            line = line.trim();
            if (!line.isEmpty()) {
                curEntry.setName(line);
            }
        }
        return curEntry;
    }

    /**
     * Data class for M3U Entries with getters & setters
     */
    public static class M3U_Entry {
        private String _tvgName, _name;
        private String _tvgLogo;
        private String _tvgLanguage;
        private String _tvgCountry;
        private String _tvgEpgUrl;
        private String _tvgUrl;
        private String _tvgId;
        private String _groupTitle;
        private String _url;
        private String[] _tags = new String[0];
        private int _seconds = -1;
        private boolean _isRadio = false;

        public void setTvgName(String value) {
            _tvgName = value;
        }

        public String getName() {
            return (_tvgName != null && !_tvgName.isEmpty()) ? _tvgName : _name;
        }

        public void setName(String value) {
            _name = value;
        }

        public String getTvgLogo() {
            return _tvgLogo;
        }

        public void setTvgLogo(String value) {
            _tvgLogo = value;
        }

        public String getUrl() {
            return _url;
        }

        public void setUrl(String value) {
            _url = value;
        }

        public int getSeconds() {
            return _seconds;
        }

        public void setSeconds(int value) {
            _seconds = value;
        }

        public String getTvgEpgUrl() {
            return _tvgEpgUrl;
        }

        public void setTvgEpgUrl(String value) {
            _tvgEpgUrl = value;
        }

        public String getTvgUrl() {
            return _tvgUrl;
        }

        public void setTvgUrl(String value) {
            _tvgUrl = value;
        }

        public String getTvgCountry() {
            return _tvgCountry;
        }

        public void setTvgCountry(String value) { _tvgCountry = value; }

        public String getTvgLanguage() {
            return _tvgLanguage;
        }

        public void setTvgLanguage(String value) { _tvgLanguage = value; }

        public boolean isRadio() {
            return _isRadio;
        }

        public String getTvgId() {
            return _tvgId;
        }

        public void setTvgId(String value) { _tvgId = value; }

        public String getGroupTitle() {
            return _groupTitle;
        }

        public void setGroupTitle(String value) {
            _groupTitle = value;
        }

        public void setIsRadio(boolean value) {
            _isRadio = value;
        }

        public String[] getTags() {
            return _tags;
        }

        public void setTags(String[] value) {
            _tags = value;
        }

        @Override
        public String toString() {
            return getName() + " " + getUrl();
        }
    }
}
