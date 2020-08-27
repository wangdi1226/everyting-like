package task;

import app.FileMeta;
import util.DBUtil;
import util.PinyinUtil;
import util.Util;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FileSave implements ScanCallback {
    @Override
    public void callback(File dir) {
        //文件夹下一级子文件和子文件夹保存到数据库
        // 获取本地目录下一级子文件和子文件夹
        // 集合框架中使用自定义类型，判断是否某个对象在集合存在：比对两个集合中的元素
        // list，set
        File[] children = dir.listFiles();
        List<FileMeta> locals = new ArrayList<>();
        if(children != null){
            for(File child : children){
                locals.add(new FileMeta(child));
            }
        }

        //获取数据库保存的dir目录的下一级子文件和子文件夹(jdbc select)
        List<FileMeta> metas = query(dir);

        // 数据库有，本地没有，做删除(delete)
        for(FileMeta meta : metas){
            if(!locals.contains(meta)){
                // meta的删除：
                // 1.删除meta信息本身
                // 2.如果meta是目录，还要将meta所有的子文件，子文件夹都删除
                delete(meta);
            }
        }

        // 本地有，数据库没有，做插入(insert)
        for(FileMeta meta : locals){
            if(!metas.contains(meta)){
                save(meta);
            }
        }
    }

    private List<FileMeta> query(File dir){
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        List<FileMeta> metas = new ArrayList<>();
        try{
            // 1.创建数据库连接
            // 两种创建数据库连接的方式:
            // 1.DriverManager 2.DataSource
            connection = DBUtil.getConnection();
            String sql = "select name, path, is_directory, size, last_modified" +
                    " from file_meta where path=?";
            // 2.创建jdbc操作命令对象statement
            // 掌握Statement和PreparedStatement的区别
            ps = connection.prepareStatement(sql);
            ps.setString(1, dir.getPath());
            // 3.执行sql语句
            rs = ps.executeQuery();
            // 4.处理结果集ResultSet
            while(rs.next()){
                String name = rs.getString("name");
                String path = rs.getString("path");
                Boolean isDirectory = rs.getBoolean("is_directory");
                Long size = rs.getLong("size");
                Timestamp lastModified = rs.getTimestamp("last_modified");
                FileMeta meta = new FileMeta(name, path, isDirectory,
                        size, new java.util.Date(lastModified.getTime()));
                System.out.printf("查询文件信息：name=%s, path=%s, is_directory=%s," +
                        " size=%s, last_modified=%s\n", name, path, String.valueOf(isDirectory),
                        String.valueOf(size), Util.parseDate(new java.util.Date(lastModified.getTime())));
                metas.add(meta);
            }
            return metas;
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException("查询文件信息出错，同学们检查sql查询语句", e);
        }finally {//5.释放资源
            DBUtil.close(connection, ps, rs);
        }
    }

    /**
     * 文件信息保存到数据库
     * @param meta
     */
    private void save(FileMeta meta){
        Connection connection = null;
        PreparedStatement statement = null;
        try {
            //1.获取数据库连接
            connection = DBUtil.getConnection();
            //添加is_directory,占位符，set值3，4，5，6 +1,添加set第3个参数is_directory
            String sql = "insert into file_meta" +
                    "(name, path, is_directory, size, last_modified, pinyin, pinyin_first)" +
                    " values (?, ?, ?, ?, ?, ?, ?)";
            //2.获取sql操作命令对象statement
            statement = connection.prepareStatement(sql);
            statement.setString(1, meta.getName());
            statement.setString(2, meta.getPath());
            statement.setBoolean(3, meta.getDirectory());
            statement.setLong(4, meta.getSize());
            // 数据库保存日期类型，可以按数据库设置的日期格式，以字符串传入
            statement.setString(5, meta.getLastModifiedText());
//            statement.setTimestamp(5,
//                    new Timestamp(meta.getLastModified().getTime()));
            statement.setString(6, meta.getPinyin());
            statement.setString(7, meta.getPinyinFirst());
            System.out.printf("insert name=%s, path=%s\n", meta.getName(), meta.getPath());
            //3.执行sql
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("文件保存失败，同学们检查一下sql insert语句", e);
        } finally {
            //4.释放资源
            DBUtil.close(connection, statement);
        }
    }

    // meta的删除：
    // 1.删除meta信息本身
    // 2.如果meta是目录，还要将meta所有的子文件，子文件夹都删除
    public void delete(FileMeta meta){
        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = DBUtil.getConnection();
            String sql = "delete from file_meta where" +
                    " (name=? and path=? and is_directory=?)"; //删除文件自身
            if(meta.getDirectory()){//如果是文件夹，还要删除文件夹的子文件和子文件夹
                sql += " or path=?" +//匹配数据库文件夹的儿子
                        " or path like ?";//匹配数据库文件夹的孙后辈
            }
            ps = connection.prepareStatement(sql);
            ps.setString(1, meta.getName());
            ps.setString(2, meta.getPath());
            ps.setBoolean(3, meta.getDirectory());
            if(meta.getDirectory()){
                ps.setString(4,
                        meta.getPath()+File.separator+meta.getName());
                ps.setString(5,
                        meta.getPath()+File.separator+meta.getName()+File.separator+"%");
            }
            System.out.printf("删除文件信息，dir=%s\n",
                    meta.getPath()+File.separator+meta.getName());
            ps.executeUpdate();
        }catch (Exception e){
            throw new RuntimeException("删除文件信息出错，同学们需要检查delete语句", e);
        }finally {
            DBUtil.close(connection, ps);
        }
    }

    public static void main(String[] args) {
//        DBInit.init();
//        File file = new File("D:\\比特科技\\10班\\板书");
//        FileSave fileSave = new FileSave();
//        fileSave.save(file);
//        fileSave.query(file.getParentFile());
        List<FileMeta> locals = new ArrayList<>();
        locals.add(new FileMeta("新建文件夹",
                "D:\\TMP\\maven-test - 副本",
                true, 0, new Date()));
        locals.add(new FileMeta("中华人民共和国",
                "D:\\TMP\\maven-test - 副本",
                true, 0, new Date()));
        locals.add(new FileMeta("阿凡达.txt",
                "D:\\TMP\\maven-test - 副本\\中华人民共和国",
                true, 0, new Date()));

        List<FileMeta> metas = new ArrayList<>();
        metas.add(new FileMeta("新建文件夹",
                "D:\\TMP\\maven-test - 副本",
                true, 0, new Date()));
        metas.add(new FileMeta("中华人民共和国2",
                "D:\\TMP\\maven-test - 副本",
                true, 0, new Date()));
        metas.add(new FileMeta("阿凡达.txt",
                "D:\\TMP\\maven-test - 副本\\中华人民共和国2",
                true, 0, new Date()));

        // 集合中是否包含某个元素，不一定代表传入这个对象在Java内存中是同一个对象的引用
        // 满足一定条件（集合中的元素类型需要重写hashCode和equals
        // ---->业务需要哪些属性来判断元素同一个），
        // list.contains方法可以返回true
//        Boolean contains = locals.contains(new FileMeta(new File("")));
        for(FileMeta meta : locals){
            if(!metas.contains(meta)){
                System.out.println(meta);
            }
        }
    }
}
