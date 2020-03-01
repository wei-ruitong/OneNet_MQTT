package com.example.onenet_mqtt;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chinamobile.iot.onenet.mqtt.MqttCallBack;
import com.chinamobile.iot.onenet.mqtt.MqttClient;
import com.chinamobile.iot.onenet.mqtt.MqttConnectOptions;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttConnAck;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttConnect;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttMessage;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttPubAck;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttPubComp;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttPublish;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttSubAck;
import com.chinamobile.iot.onenet.mqtt.protocol.MqttSubscribe;
import com.chinamobile.iot.onenet.mqtt.protocol.imp.QoS;
import com.chinamobile.iot.onenet.mqtt.protocol.imp.Type;

import java.io.IOException;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
private TextView textView,temp_tv,humi_tv,led_status_tv;
private Button button,button1,button2;
private Toolbar toolbar;
private   Typeface iconfont;

private Handler handler=new Handler(){
    @Override
    public void handleMessage(@NonNull Message msg) {
      if(msg.what==0) {
          toolbar.setTitle("在线");
          MqttSubscribe mqttSubscribe1 = new MqttSubscribe("LED_STATUS", QoS.AT_LEAST_ONCE);
          MqttClient.getInstance().subscribe(mqttSubscribe1);
          MqttSubscribe mqttSubscribe = new MqttSubscribe("temp_humi", QoS.AT_LEAST_ONCE);
          MqttClient.getInstance().subscribe(mqttSubscribe);
      }else{
          toolbar.setTitle("离线");
       }
    }
};
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        iconfont = Typeface.createFromAsset(getAssets(), "iconfont.ttf");

        //改函数用来初始化控件
        init_KJ();
        led_status_tv.setTypeface(iconfont);
        init_MQtt();
        //按键控制开关灯
        button.setOnClickListener(this);
        button1.setOnClickListener(this);
        button2.setOnClickListener(this);
    }
    /**
     * 按键点击响应事件
     * @param view
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_on:
                byte[] data_on = {'1'};
                MqttPublish mqttPublish_on=new MqttPublish("/mqtt/topic/0",data_on,QoS.AT_LEAST_ONCE);
                MqttClient.getInstance().sendMsg(mqttPublish_on);
                break;
            case R.id.btn_off:
                byte[] data_off = {'0'};
                MqttPublish mqttPublish_off=new MqttPublish("/mqtt/topic/0",data_off,QoS.AT_LEAST_ONCE);
                MqttClient.getInstance().sendMsg(mqttPublish_off);
                break;
            case R.id.sub_topic:
               /* String topic=editText.getText().toString();
               */
               opens(view);

        }
    }

    /**
     * 初始化控件
     */
    private void init_KJ(){
        toolbar=(Toolbar)findViewById(R.id.toolbar);
        button=(Button)findViewById(R.id.btn_on);
        button1=(Button)findViewById(R.id.btn_off);
        button2=(Button)findViewById(R.id.sub_topic);
        textView = (TextView) findViewById(R.id.text1);
        temp_tv=(TextView)findViewById(R.id.temp);
        humi_tv=(TextView)findViewById(R.id.humi);
        led_status_tv=(TextView)findViewById(R.id.led_status);
    }

    /**
     * MQTT连接服务器
     */
    private void init_MQtt(){
        //初始化sdk
        MqttClient.initialize(this,"183.230.40.39",6002,"586234185","262685","HE4LCsIsYxF7WkDPVSV4ua7isfw=");
        //设置接受响应回调
        MqttClient.getInstance().setCallBack(callBack);
        //设置连接属性
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setCleanSession(true);
        connectOptions.setKeepAlive(121);
        connectOptions.setWill(false);
        connectOptions.setWillQoS(QoS.AT_MOST_ONCE);
        connectOptions.setWillRetain(false);
        //建立TCP连接
        MqttClient.getInstance().connect(connectOptions);
    }

    /**
     * MQTT回调函数
     */
    private MqttCallBack callBack =new MqttCallBack() {
        @Override
        public void messageArrived(MqttMessage mqttMessage) {
            switch (mqttMessage.getMqttHeader().getType()){
                case CONNACK:
                     MqttConnAck mqttConnAck = (MqttConnAck) mqttMessage;
                     Message message=new Message();
                     message.what= mqttConnAck.getConnectionAck();
                     handler.sendMessage(message);
                    break;
                case PUBLISH:
                    MqttPublish mqttPublish = (MqttPublish) mqttMessage;
                    byte[] data = mqttPublish.getData();
                    String topic= mqttPublish.getTopicName();
                    String s=new String(data);
                    switch (topic){

                        case "temp_humi":
                            temp_tv.setText(s.substring(0,2)+"℃");
                            humi_tv.setText(s.substring(2,4)+"%");
                            break;
                        case "LED_STATUS":
                            if(s.equals("1")) {
                                led_status_tv.setTypeface(iconfont);
                                led_status_tv.setTextColor(Color.parseColor("#3700B3"));
                            } else {
                                led_status_tv.setTypeface(iconfont);
                                led_status_tv.setTextColor(Color.parseColor("#c0c0c0"));
                            }
                            break;
                        default:
                              textView.setText(topic+" "+new String(data));
                    }
                    break;
                case SUBSCRIBE:
                    MqttSubscribe mqttSubscribe=(MqttSubscribe)mqttMessage;
                    try {
                        textView.setText(new String(mqttSubscribe.getPacket()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;

                case SUBACK:
                    MqttSubAck mqttSubAck = (MqttSubAck) mqttMessage;
                    break;
                case PINGRESP:
                    break;
                case PUBACK:
                    MqttPubAck mqttPubAck=(MqttPubAck) mqttMessage;

                    break;
                case PUBCOMP:
                    break;
            }
        }
        @Override
        public void connectionLost(Exception e) {

        }
        @Override
        public void disconnect() {

        }
    };

    /**
     * 自定义对话框
     * @param view
     */
    public  void opens(View view){
        // 加载布局
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog,null);
        final EditText editText=(EditText) dialogView.findViewById(R.id.edit1);
        final String topic = editText.getText().toString();
        new AlertDialog.Builder(view.getContext()) // 使用android.support.v7.app.AlertDialog
                .setView(dialogView) // 设置布局
                .setCancelable(true) // 设置点击空白处不关闭
                .setNegativeButton("确定", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
                    @Override public void onClick(DialogInterface dialog, int which) {
                       /* if(topic==""){
                           textView.setText("TOPIC不能为空！请重新订阅");
                        }else {
                            MqttSubscribe mqttSubscribe = new MqttSubscribe(topic, QoS.AT_LEAST_ONCE);
                            MqttClient.getInstance().subscribe(mqttSubscribe);
                            textView.setText("订阅成功");
                        }*/
                       textView.setText(topic);
                        setDialogIsShowing(dialog, true); // 设置关闭
                    }
                }) // 设置取消按钮，并设置监听事件

                .create() // 创建对话框
                .show(); // 显示对话框
    }

    /**
     * 设置对话框是否显示
     * @param dialog 对话框
     * @param isClose 是否显示. true为关闭，false为不关闭
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void setDialogIsShowing(DialogInterface dialog, boolean isClose) {
        try{
            // 获取到android.app.Dialog类
            Field mShowing = dialog.getClass().getSuperclass().getSuperclass().getDeclaredField("mShowing");
            mShowing.setAccessible(true); // 设置可访问
            mShowing.set(dialog,isClose); // 设置是否关闭
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
