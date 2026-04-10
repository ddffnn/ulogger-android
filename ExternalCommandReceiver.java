/*
 * Copyright (c) 2019 Bartek Fabiszewski
 * http://www.fabiszewski.net
 *
 * This file is part of μlogger-android.
 * Licensed under GPL, either version 3, or any later.
 * See <http://www.gnu.org/licenses/>
 */

package net.fabiszewski.ulogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import net.fabiszewski.ulogger.db.DbAccess;
import net.fabiszewski.ulogger.services.LoggerService;
import net.fabiszewski.ulogger.services.WebSyncService;
import net.fabiszewski.ulogger.ui.AutoNamePreference;
import net.fabiszewski.ulogger.ui.SettingsActivity;
import net.fabiszewski.ulogger.utils.BroadcastHelper;

public class ExternalCommandReceiver extends BroadcastReceiver {

    private static final String START_LOGGER = "start logger";
    private static final String START_NEW_LOGGER = "start new logger";
    private static final String STOP_LOGGER = "stop logger";
    private static final String START_UPLOAD = "start upload";
    private static final String GET_TRACK_ID = "get track id";

    public static final String BROADCAST_TRACK_ID = "net.fabiszewski.ulogger.broadcast.track_id";
    public static final String EXTRA_TRACK_ID = "trackId";

    @Override
    public void onReceive(@NonNull Context context, @Nullable Intent intent) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean allowExternal = prefs.getBoolean(SettingsActivity.KEY_ALLOW_EXTERNAL, false);
        if (!allowExternal) {
            return;
        }
        if (intent != null) {
            String command = intent.getStringExtra("command");
            boolean overwrite = intent.getBooleanExtra("overwrite", true);
            if (command != null) {
                switch (command) {
                    case START_LOGGER -> startLoggerService(context);
                    case START_NEW_LOGGER -> startNewLoggerService(context, overwrite);
                    case STOP_LOGGER -> stopLogger(context);
                    case START_UPLOAD -> uploadData(context);
                    case GET_TRACK_ID -> sendTrackId(context);
                }
            }
        }
    }

    /**
     * Start logger service forcing new track
     * @param context Context
     */
    private void startNewLoggerService(@NonNull Context context, boolean overwrite) {
        if (overwrite || !DbAccess.needsSync(context)) {
            DbAccess.newTrack(context, AutoNamePreference.getAutoTrackName(context));
            Intent intent = new Intent(context, LoggerService.class);
            ContextCompat.startForegroundService(context, intent);
        }
    }

    /**
     * Start logger service
     * @param context Context
     */
    private void startLoggerService(@NonNull Context context) {
        DbAccess.newAutoTrack(context);
        Intent intent = new Intent(context, LoggerService.class);
        ContextCompat.startForegroundService(context, intent);
    }

    /**
     * Stop logger service
     * @param context Context
     */
    private void stopLogger(@NonNull Context context) {
        Intent intent = new Intent(context, LoggerService.class);
        context.stopService(intent);
    }

    /**
     * Start logger service
     * @param context Context
     */
    private void uploadData(@NonNull Context context) {
        if (DbAccess.needsSync(context)) {
            Intent intent = new Intent(context, WebSyncService.class);
            ContextCompat.startForegroundService(context, intent);
        }
    }

    /**
     * Send current track id via broadcast
     * @param context Context
     */
    private void sendTrackId(@NonNull Context context) {
        int trackId = DbAccess.getTrackId(context);
        Bundle extras = new Bundle();
        extras.putInt(EXTRA_TRACK_ID, trackId);
        BroadcastHelper.sendBroadcast(context, BROADCAST_TRACK_ID, extras);
    }
}