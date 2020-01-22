package com.ray.lab.voice.util;

import android.os.AsyncTask;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpHelper {
    private static final String TAG = "HttpHelper";
    private HttpListener listener;
    public interface HttpListener{
        void onHttpSuccess(String data);
        void onHttpFail();
    }
    public HttpHelper(HttpListener listener){
        this.listener=listener;
    }

    //get请求
    public HttpHelper get(String url){
        Log.d(TAG, url);
        doHttp(url,"GET","");
        return this;
    }
    //post
    public HttpHelper  post(String url,String string){
        doHttp(url,"POST",string);
        return this;
    }

    public void doHttp(String url,String method,String string){
        new MyAsyncTask(url,method,string).execute();
    }


    private class MyAsyncTask extends AsyncTask<String,Integer,String> {
        private String pathUrl,method,string;
        public MyAsyncTask(String pathUrl,String method,String string){
            this.pathUrl=pathUrl;
            this.method=method;
            this.string=string;
        }

        //在子线程里执行
        @Override
        protected String doInBackground(String... strings) {
            String data="";
            try {
                URL url=new URL(pathUrl);
                HttpURLConnection connection= (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
                connection.setConnectTimeout(5000);

                if("POST".equals(method)){
                    PrintWriter printWriter=new PrintWriter(connection.getOutputStream());
                    printWriter.write(string);
                    printWriter.flush();
                    printWriter.close();
                }

                connection.connect();
                int code=connection.getResponseCode();
                if(code==HttpURLConnection.HTTP_OK){
                    InputStream is= connection.getInputStream();
                    data=convertStream2String(is);
                }else{
                    data="0";
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }


            return data;
        }


        //在主线程里执行
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if("0".equals(s)){//请求失败
                listener.onHttpFail();
            }else{
                listener.onHttpSuccess(s);
            }

        }
    }

    public  String convertStream2String(InputStream input){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();// 自带缓存的输出流
        int len=-1;
        byte [] buffer = new byte[512];
        try {
            while((len = input.read(buffer))!=-1){
                baos.write(buffer, 0, len); // 将读到的字节，写入baos
            }
            return new String(baos.toByteArray());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
    }
}
