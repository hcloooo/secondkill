package com.nacl.secondkill.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.nacl.secondkill.entity.User;
import com.nacl.secondkill.vo.RespBean;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class UserUtil {
    public static void main(String[] args) throws Exception {
        createUser(5000);
    }

    private static void createUser(int count) throws Exception {
        List<User> users = new ArrayList<>(count);
        for(int i = 0; i < count; ++i) {
            User user = new User();
            user.setId(13000000000l + i);
            user.setNickname("user" + i);
            user.setPassword("e3a4ca3d89ff8439458fa27e4a52106f");
            user.setSalt("hexiangdong");
            user.setLoginCount(0);
            users.add(user);
        }
        System.out.println("创建user");
//        Connection connection = getConnect();
//        System.out.println("创建数据库连接");
//        String sql = "insert into t_user(login_count,nickname,salt,password,id) values(?,?,?,?,?)";
//        PreparedStatement preparedStatement = connection.prepareStatement(sql);
//        for(int i = 0; i < count; ++i) {
//            User user = users.get(i);
//            preparedStatement.setInt(1, user.getLoginCount());
//            preparedStatement.setString(2, user.getNickname());
//            preparedStatement.setString(3, user.getSalt());
//            preparedStatement.setString(4, user.getPassword());
//            preparedStatement.setLong(5,user.getId());
//            preparedStatement.addBatch();
//        }
//        preparedStatement.executeBatch();
//        preparedStatement.close();
//        System.out.println("插入成功");
//        connection.close();
//        System.out.println("放入数据库");
        String urlString = "http://localhost:8000/login/doLogin";
        File file = new File("C:\\Users\\86188\\Desktop\\config.txt");
        if (file.exists()) {
            file.delete();
        }
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        file.createNewFile();
        raf.seek(0);
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            URL url = new URL(urlString);
            HttpURLConnection co = (HttpURLConnection) url.openConnection();
            co.setRequestMethod("POST");
            co.setDoOutput(true);
            OutputStream out = co.getOutputStream();
            String params = "mobile=" + user.getId() + "&password=" + MD5Util.inputPassToFromPass("123456");
            out.write(params.getBytes());
            out.flush();
            InputStream inputStream = co.getInputStream();
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            byte buff[] = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(buff)) >= 0) {
                bout.write(buff, 0, len);
            }
            inputStream.close();
            bout.close();
            String response = new String(bout.toByteArray());
            ObjectMapper mapper = new ObjectMapper();
            RespBean respBean = mapper.readValue(response, RespBean.class);
            String userTicket = ((String) respBean.getObj());
            System.out.println("create userTicket : " + user.getId());

            String row = user.getId() + "," + userTicket;
            raf.seek(raf.length());
            raf.write(row.getBytes());
            raf.write("\r\n".getBytes());
            System.out.println("write to file : " + user.getId());
        }
        raf.close();

        System.out.println("over");

    }
    public static Connection getConnect() throws Exception {
        String url = "jdbc:mysql://101.132.146.181:3306/seckill?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "Hh545466093";
        String driver = "com.mysql.cj.jdbc.Driver";
        Class.forName(driver);
        return DriverManager.getConnection(url, username, password);
    }


}
