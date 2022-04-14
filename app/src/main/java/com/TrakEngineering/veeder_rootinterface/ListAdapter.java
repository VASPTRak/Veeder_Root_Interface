package com.TrakEngineering.veeder_rootinterface;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.TrakEngineering.veeder_rootinterface.enity.WifiEntityClass;

import java.util.List;


class ListAdapter extends ArrayAdapter<WifiEntityClass> {
    private final Activity mActivity;
    private final List<WifiEntityClass> wifiName;

    public ListAdapter(Activity a, List<WifiEntityClass> wifiName) {
        super(a, R.layout.single_list, wifiName);
        mActivity = a;
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        this.wifiName = wifiName;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View gridView=null;

        if (convertView == null) {


            gridView = inflater.inflate(R.layout.single_list, null);

            TextView wifiProvider = gridView.findViewById(R.id.txt_wifi_provider);
            TextView wifiStatus = gridView.findViewById(R.id.wifiConnStatus);

            String connectionStatus = wifiName.get(position).connectionStatus;

            if (connectionStatus.equalsIgnoreCase("Connected")) {
                wifiStatus.setBackgroundColor(mActivity.getResources().getColor(R.color.Connected));
            } else {
                wifiStatus.setBackgroundColor(mActivity.getResources().getColor(R.color.Disonnected));
            }

            wifiProvider.setText(wifiName.get(position).ssidName);
            wifiStatus.setText(connectionStatus);

        } else {

            gridView = convertView;
        }

        return gridView;

    }

}
