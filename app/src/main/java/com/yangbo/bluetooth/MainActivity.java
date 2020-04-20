package com.yangbo.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, ConnectButtonClick {

    // EditText
    private EditText editText;

    // Button
    private Button openBtBtn;
    private Button closeBtBtn;
    private Button findBtBtn;
    private Button disconnectBtn;
    private Button sendTextBtn;
    private Button leftTextBtn;
    private Button upTextBtn;
    private Button downTextBtn;

    // ListView
    private ListView listView;
    private Map<String, Object> map;
    private ArrayList<String> addressArrayList;
    private ArrayList<Map<String, Object>> deviceArrayList;
    private MyAdapter myAdapter;

    // Bluetooth
    private BluetoothSocket bluetoothSocket;
    private BluetoothDevice bluetoothDevice;
    private BluetoothAdapter bluetoothAdapter;

    // 输出流
    private OutputStream os = null;

    // 客户端连接
    private ConnectThread clientConnectThread;

    // 常量
    public static final String BLUETOOTH_NO_NAME = "No Name";
    public static final String BLUETOOTH_LOGO = "bluetooth_logo";
    public static final String BLUETOOTH_NAME = "bluetooth_name";
    public static final String BLUETOOTH_BOND = "bluetooth_bond";
    public static final String BLUETOOTH_CONNECT = "bluetooth_connect";
    public static final String BLUETOOTH_MAC = "bluetooth_mac";
    private final String BLUETOOTH_BOND_STATE_0 = "未配对";
    private final String BLUETOOTH_BOND_STATE_1 = "已配对";
    private final String BLUETOOTH_BOND_STATE_2 = "正在配对...";
    private final String BLUETOOTH_CONNECT_STATE_0 = "未连接";
    private final String BLUETOOTH_CONNECT_STATE_1 = "已连接";
    private final int END_BYTE = 0x0D;
    private final int START_BYTE_STATIC = 0x01;
    private final int START_BYTE_LEFT = 0x02;
    private final int START_BYTE_UP = 0x03;
    private final int START_BYTE_DOWN = 0x04;
    private final int REQUEST_BLUETOOTH_PERMISSION = 10;

    /**
     *********************************************
     * 函数名: onCreate
     * 功  能：创建APP界面，并进行各类变量初始化
     * 参  数：Bundle savedInstanceState
     * 返回值: 无
     *********************************************
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initApp();
    }

    // 初始化
    /**
     *********************************************
     * 函数名: initApp
     * 功  能：进行各类变量初始化
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    private void initApp() {
        // 编辑字符框初始化
        editText = findViewById(R.id.edit_text);

        // 按钮初始化、设置监听事件
        openBtBtn = findViewById(R.id.open_button);
        closeBtBtn = findViewById(R.id.close_button);
        findBtBtn = findViewById(R.id.find_button);
        disconnectBtn = findViewById(R.id.disconnect_bluetooth_button);
        sendTextBtn = findViewById(R.id.send_button_static);
        leftTextBtn = findViewById(R.id.send_button_left);
        upTextBtn = findViewById(R.id.send_button_up);
        downTextBtn = findViewById(R.id.send_button_down);
        openBtBtn.setOnClickListener(this);
        closeBtBtn.setOnClickListener(this);
        findBtBtn.setOnClickListener(this);
        disconnectBtn.setOnClickListener(this);
        sendTextBtn.setOnClickListener(this);
        leftTextBtn.setOnClickListener(this);
        upTextBtn.setOnClickListener(this);
        downTextBtn.setOnClickListener(this);


        // 获得本地蓝牙设备
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // ListView 数据存储初始化
        listView = findViewById(R.id.list_view);
        addressArrayList = new ArrayList<String>(); // 存储蓝牙地址用来查重和获取远程蓝牙设备
        deviceArrayList = new ArrayList<Map<String, Object>>(); // map 数据添加到这里面 然后作为MyAdapter 的数据源

        // 自定义ListView 适配器 MyAdapter
        myAdapter = new MyAdapter(this);
        myAdapter.setList(deviceArrayList);
        myAdapter.setButtonClick(this);

        // 适配到ListView
        listView.setAdapter(myAdapter);

        // 获取运行时蓝牙授权 注册广播
        registerReceiver();
        requestBluetoothPermission();

        // 程序启动时启动蓝牙
        openBluetooth();
    }

    /**
     *********************************************
     * 函数名: onClick
     * 功  能：响应除"点击连接"外所有按钮的响应事件
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    @Override
    public void onClick(View button) {
        switch (button.getId()) {
            case R.id.open_button:
                openBluetooth();
                break;
            case R.id.close_button:
                closeBluetooth();
                break;
            case R.id.find_button:
                findBluetooth();
                break;
            case R.id.disconnect_bluetooth_button:
                try {
                    bluetoothSocket.close();
                } catch (Exception e){
                    Log.e("phil","Socket 关闭失败！");
                }
                break;
            case R.id.send_button_static:
                sendTextToRemoteBluetooth(START_BYTE_STATIC);
                break;
            case R.id.send_button_left:
                sendTextToRemoteBluetooth(START_BYTE_LEFT);
                break;
            case R.id.send_button_up:
                sendTextToRemoteBluetooth(START_BYTE_UP);
                break;
            case R.id.send_button_down:
                sendTextToRemoteBluetooth(START_BYTE_DOWN);
                break;
            default:
                break;
        }
    }

    /**
     *********************************************
     * 函数名: connectBtnClick
     * 功  能：响应"点击连接"按钮点击事件，并启动蓝牙连接
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    @Override
    public void connectBtnClick(int position) {
        bluetoothAdapter.cancelDiscovery(); // 取消搜索
        // 查看蓝牙是否开启
        if(bluetoothAdapter.isEnabled()) {
            bluetoothDevice = bluetoothAdapter.getRemoteDevice(addressArrayList.get(position)); // 获取远端蓝牙设备
            Map<String, Object> map_connect = deviceArrayList.get(position);
            if(map_connect.get(BLUETOOTH_CONNECT) == BLUETOOTH_CONNECT_STATE_0){
                clientConnectThread = new ConnectThread();
                clientConnectThread.start();
            } else{
                Toast.makeText(this, "此设备已连接，请不要重复连接！", Toast.LENGTH_SHORT).show();
            }
        } else{
            Toast.makeText(this, "请先开启蓝牙！", Toast.LENGTH_SHORT).show();
        }
    }

    // 开启蓝牙
    /**
     *********************************************
     * 函数名: openBluetooth()
     * 功  能：开启系统蓝牙
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    private void openBluetooth() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "此设备不支持蓝牙功能！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 1);
        } else {
            Toast.makeText(this, "蓝牙已开启！", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    // 响应拒绝蓝牙开启请求
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "此程序需要蓝牙权限！", Toast.LENGTH_SHORT).show();
        }
    }

    // 关闭蓝牙
    /**
     *********************************************
     * 函数名: closeBluetooth
     * 功  能：关闭系统蓝牙
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    private void closeBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
            Toast.makeText(this, "已关闭蓝牙!", Toast.LENGTH_SHORT).show();
        }
    }

    // 搜索蓝牙
    /**
     *********************************************
     * 函数名: findBluetooth
     * 功  能：搜索远程蓝牙设备
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    private void findBluetooth() {
        bluetoothAdapter.cancelDiscovery(); // 取消搜索
        bluetoothAdapter.startDiscovery(); // 开始搜索
        Toast.makeText(this, "搜索中... 请稍后...", Toast.LENGTH_SHORT).show();
    }

    // 向蓝牙发送数据
    /**
     *********************************************
     * 函数名: sendTextToRemoteBluetooth
     * 功  能：向连接的蓝牙设备输出字节流，字头带屏幕显示方式
     * 参  数：int start_byte
     * 返回值: 无
     *********************************************
     */
    protected void sendTextToRemoteBluetooth(int start_byte) {
        bluetoothAdapter.cancelDiscovery();
        try {
            if (os != null) {

                if(editText.length() != 0){
                    os.write(start_byte);
                    os.write(editText.getText().toString().getBytes("gbk")); // 在Windows 串口助手上要使用gbk 编码
                    os.write(END_BYTE);
                } else{
                    Toast.makeText(this, "请先在文字输入框中输入字符！", Toast.LENGTH_SHORT).show();
                }
                Log.i("phil", "写数据到：" + bluetoothDevice.getName() + "_" + editText.getText().toString());

            }
        } catch (Exception e) {
            Toast.makeText(this, "请先点击连接蓝牙设备！", Toast.LENGTH_SHORT).show();
            Log.e("phil", "写数据失败");
        }
    }

    // 注册蓝牙广播
    /**
     *********************************************
     * 函数名: sregisterReceiver
     * 功  能：注册蓝牙状态广播监听器
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND); // 搜索到新设备广播
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED); // 蓝牙已连接广播
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); // 蓝牙断开广播
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED); // 配对状态改变广播
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); // 搜索结束广播
        registerReceiver(bluetoothReceiver, filter);
    }

    // 创建蓝牙广播接收器
    /**
     *********************************************
     * 实  例: new BroadcastReceiver() // 内部函数
     * 功  能：注册蓝牙状态广播监听器
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    public final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // 搜索到新设备
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceAddress = device.getAddress();

                // 检查是否已经搜索到过此设备
                if (!addressArrayList.contains(deviceAddress)) {
                    addressArrayList.add(deviceAddress);

                    // 数据源
                    map = new HashMap<String, Object>();
                    map.put(BLUETOOTH_LOGO, R.drawable.bluetooth);

                    // 检查是否设备名为 null
                    if (deviceName != null) {
                        map.put(BLUETOOTH_NAME, deviceName);
                    } else {
                        map.put(BLUETOOTH_NAME, BLUETOOTH_NO_NAME);
                    }

                    // 检查是否配对
                    if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                        map.put(BLUETOOTH_BOND, BLUETOOTH_BOND_STATE_0);
                    } else {
                        map.put(BLUETOOTH_BOND, BLUETOOTH_BOND_STATE_1);
                    }

                    map.put(BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_STATE_0); // !!! 如何检测当前设备是否已经连接？
                    map.put(BLUETOOTH_MAC, deviceAddress);
                    deviceArrayList.add(map);    // 添加数据到deviceArrayList
                    myAdapter.notifyDataSetChanged();
                }
            }

            // 监听连接
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceAddress = device.getAddress();

                if (addressArrayList.contains(deviceAddress)) {
                    int devicePosition = addressArrayList.indexOf(deviceAddress);
                    map = deviceArrayList.get(devicePosition);
                    map.put(BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_STATE_1);
                    deviceArrayList.set(devicePosition, map);
                    myAdapter.notifyDataSetChanged();
                }
            }

            // 监听断开
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceAddress = device.getAddress();

                if (addressArrayList.contains(deviceAddress)) {
                    int devicePosition = addressArrayList.indexOf(deviceAddress);
                    map = deviceArrayList.get(devicePosition);
                    map.put(BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_STATE_0);
                    deviceArrayList.set(devicePosition, map);
                    myAdapter.notifyDataSetChanged();
                }
            }

            // 配对状态改变
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceAddress = device.getAddress();

                int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                switch (state) {
                    // 配对失败
                    case BluetoothDevice.BOND_NONE:
                        if (addressArrayList.contains(deviceAddress)) {
                            int devicePosition = addressArrayList.indexOf(deviceAddress);
                            map = deviceArrayList.get(devicePosition);
                            map.put(BLUETOOTH_BOND, BLUETOOTH_BOND_STATE_0);
                            deviceArrayList.set(devicePosition, map);
                            myAdapter.notifyDataSetChanged();
                        }
                        break;

                    // 正在配对
                    case BluetoothDevice.BOND_BONDING:
                        if (addressArrayList.contains(deviceAddress)) {
                            int devicePosition = addressArrayList.indexOf(deviceAddress);
                            map = deviceArrayList.get(devicePosition);
                            map.put(BLUETOOTH_BOND, BLUETOOTH_BOND_STATE_2);
                            deviceArrayList.set(devicePosition, map);
                            myAdapter.notifyDataSetChanged();
                        }
                        break;

                    // 配对成功
                    case BluetoothDevice.BOND_BONDED:
                        if (addressArrayList.contains(deviceAddress)) {
                            int devicePosition = addressArrayList.indexOf(deviceAddress);
                            map = deviceArrayList.get(devicePosition);
                            map.put(BLUETOOTH_BOND, BLUETOOTH_BOND_STATE_1);
                            deviceArrayList.set(devicePosition, map);
                            myAdapter.notifyDataSetChanged();
                        }
                        break;

                    default:
                        break;
                }
            }

            // 搜索结束
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(MainActivity.this, "蓝牙搜索完成!", Toast.LENGTH_SHORT).show();
            }
        }
    };

    /**
     *********************************************
     * 类  名: ConnectThread
     * 功  能：获得蓝牙连接socket,并发起远程连接线程
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    private class ConnectThread extends Thread {

        BluetoothSocket localSocket = null;

        public ConnectThread() {
            BluetoothSocket tmp = null;
            try {
                // Android 4.2 以前使用此方法 tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                // 安卓系统4.2以后的蓝牙通信端口为 1 ，但是默认为 -1，所以只能通过反射修改，才能成功
                tmp = (BluetoothSocket) bluetoothDevice.getClass().getDeclaredMethod("createRfcommSocket", new Class[]{int.class}).invoke(bluetoothDevice, 1);
                Log.i("phil", "正在连接到 = " + bluetoothDevice.getName() + " " + bluetoothDevice.getAddress());
            } catch (Exception e) {
                Log.e("phil", "获取 Socket 失败!", e);
            }
            localSocket = tmp;
        }

        public void run() {
            bluetoothAdapter.cancelDiscovery();
            try {
                localSocket.connect();
                // 连接成功 才将localSocket赋给 bluetoothSocket全局变量
                bluetoothSocket = localSocket;
                os = bluetoothSocket.getOutputStream();
            } catch (IOException connectException) {
                Log.e("phil", "无法连接此设备!", connectException);
                try {
                    localSocket.close();
                } catch (IOException closeException) {
                    Log.e("phil", "Socket 关闭失败!", closeException);
                }
                return;
            }
        }
    }

    // 运行时蓝牙权限授权
    /**
     *********************************************
     * 函数名: requestBluetoothPermission
     * 功  能：获得运行时蓝牙授权
     * 参  数：无
     * 返回值: 无
     *********************************************
     */
    private void requestBluetoothPermission() {

        //判断系统版本
        if (Build.VERSION.SDK_INT >= 23) {

            //检测当前app是否拥有某个权限
            int checkCallPhonePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

            //判断这个权限是否已经授权过
            if (checkCallPhonePermission != PackageManager.PERMISSION_GRANTED) {
                //判断是否需要 向用户解释，为什么要申请该权限
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    Toast.makeText(MainActivity.this, "需要蓝牙权限！", Toast.LENGTH_SHORT).show();
                }
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_BLUETOOTH_PERMISSION);
                return;
            }
        }
    }
}
