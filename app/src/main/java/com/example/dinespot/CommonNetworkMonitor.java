package com.example.dinespot;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public class CommonNetworkMonitor {

    public interface NetworkStateListener {
        void onNetworkAvailable();
        void onNetworkLost();
    }

    private final ConnectivityManager connectivityManager;
    private final NetworkStateListener listener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@NonNull Network network) {
            mainHandler.post(listener::onNetworkAvailable);
        }

        @Override
        public void onLost(@NonNull Network network) {
            mainHandler.post(listener::onNetworkLost);
        }
    };

    public CommonNetworkMonitor(Context context, NetworkStateListener listener) {
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.listener = listener;
    }

    public void start() {
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
        
        // Initial check
        if (!isConnected()) {
            listener.onNetworkLost();
        }
    }

    public void stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

    public boolean isConnected() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
