package com.yangbo.bluetooth;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import static com.yangbo.bluetooth.MainActivity.BLUETOOTH_BOND;
import static com.yangbo.bluetooth.MainActivity.BLUETOOTH_CONNECT;
import static com.yangbo.bluetooth.MainActivity.BLUETOOTH_LOGO;
import static com.yangbo.bluetooth.MainActivity.BLUETOOTH_MAC;
import static com.yangbo.bluetooth.MainActivity.BLUETOOTH_NAME;

/**
 *********************************************
 * 类  名: MyAdapter
 * 功  能：作为自定义ListView的自定义适配器
 * 参  数：无
 * 返回值: 无
 *********************************************
 */
public class MyAdapter extends BaseAdapter {

    ArrayList<Map<String, Object>> list;
    LayoutInflater inflater; // 布局反射器 从 xml 反射成对象
    ConnectButtonClick buttonClick;

    public MyAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setList(ArrayList<Map<String, Object>> list) {
        this.list = list;
    }

    public void setButtonClick(ConnectButtonClick buttonClick) {
        this.buttonClick = buttonClick;
    }

    @Override
    // 获取 listview 元素个数
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     *********************************************
     * 类  名: getView
     * 功  能：将自定义的ListView内容显示出来
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {

        // 每刷新一行listview item 都要调用一次 getView
        // 而 inflater 和 findViewById 都是耗时操作 导致性能下降
        // inflater 耗时用 converView 解决，把view 替换为 converView（Recycler视图缓存=当前屏幕显示条数+1）
        // findViewById 耗时用 ViewHolder类解决 ， 必须在converView优化情况下使用

        ViewHolder holder = null;

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.listview_item, null); // null 没必要提供根结点？
            holder = new ViewHolder();

            holder.imageView = (ImageView)convertView.findViewById(R.id.bluetooth_logo);
            holder.textViewName = (TextView)convertView.findViewById(R.id.bluetooth_name);
            holder.textViewBond = (TextView)convertView.findViewById(R.id.bluetooth_bond);
            holder.textViewConnect = (TextView)convertView.findViewById(R.id.bluetooth_connect);
            holder.textViewMac = (TextView)convertView.findViewById(R.id.bluetooth_mac);
            holder.button = (Button)convertView.findViewById(R.id.bluetooth_button);
            convertView.setTag(holder); //关联 holder 和 convertview
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Map map = list.get(position);
        holder.imageView.setImageResource((Integer)map.get(BLUETOOTH_LOGO));
        holder.textViewName.setText((String)map.get(BLUETOOTH_NAME));
        holder.textViewBond.setText((String)map.get(BLUETOOTH_BOND));
        holder.textViewConnect.setText((String)map.get(BLUETOOTH_CONNECT));
        holder.textViewMac.setText((String)map.get(BLUETOOTH_MAC));

        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 点击按钮这里首先被调用 因为是这里实现的按钮点击监听
                // 然后通过 ConnnectBtnClick 接口对象将参数传递到接口内部的相关函数
                // 接口具体实现在 MainActivity 中 实现，实现的时候可以得到这里传入的参数
                // 通过这样使用接口的方法就可以调用 MainActivity 中的资源
                buttonClick.connectBtnClick(position);
            }
        });

        return convertView;
    }

    public class ViewHolder{
        ImageView imageView;
        TextView textViewName;
        TextView textViewBond;
        TextView textViewConnect;
        TextView textViewMac;
        Button button;
    }
}
