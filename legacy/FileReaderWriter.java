import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by xwhqs on 2016/3/15.
 */
public class FileReaderWriter {
    public static boolean createNewFile(String filePath) {
        boolean isSuccess = true;

        //如有则将“\\”转换成“/”，没有则不产生任何变化
        String filePathTurn = filePath.replaceAll("\\\\", "/");

        //先过滤掉文件名
        int index = filePathTurn.lastIndexOf("/");
        String dir = filePathTurn.substring(0, index);

        //再创建文件夹
        File fileDir = new File(dir);
        isSuccess = fileDir.mkdirs();

        //创建文件
        File file = new File(filePathTurn);
        try {
            isSuccess = file.createNewFile();
        } catch (IOException e) {
            isSuccess = false;
            e.printStackTrace();
        }

        return false;
    }

    public static boolean writeIntoFile(String content, String filePath, boolean isAppend) {
        boolean isSuccess = true;

        //先过滤掉文件名
        int index = filePath.lastIndexOf("/");
        String dir = filePath.substring(0, index);

        //创建文件路径
        File fileDir = new File(dir);
        fileDir.mkdirs();

        //再创建路径下咋文件
        File file = null;
        try {
            file = new File(filePath);
            file.createNewFile();
        } catch (IOException e) {
            isSuccess = false;
            e.printStackTrace();
        }

        //写入文件
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(file, isAppend);
            fileWriter.write(content);
            fileWriter.flush();
        } catch (IOException e) {
            isSuccess = false;
            e.printStackTrace();
        } finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return false;
    }

}
