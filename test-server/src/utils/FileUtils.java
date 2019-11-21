package utils;

import explorer.scheduler.FailureInjectingScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class FileUtils {
  private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

  public static void writeToFile(String fileName, String content, boolean append) {
    try (FileWriter fw = new FileWriter(fileName, append); PrintWriter pw = new PrintWriter(fw)) {
      //File f = new File(fileName);
      //f.createNewFile(); // if file already exists will do nothing
      pw.println(content);
      pw.flush();
      fw.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void cleanFile(String fileName) {
    PrintWriter writer = null;
    try {
      writer = new PrintWriter(fileName);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    writer.print("");
    writer.close();
  }

  public static void removeFirstLineOfFile(String fileName) {
    try {
    File path = new File(fileName);
    Scanner scanner = new Scanner(path);
    ArrayList<String> coll = new ArrayList<String>();
    scanner.nextLine();
    while (scanner.hasNextLine()) {
      String line = scanner.nextLine();
      coll.add(line);
    }

    scanner.close();

    FileWriter writer = new FileWriter(path);
    for (String line : coll) {
      writer.write(line.concat("\n"));
    }

    writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  public static List<String> readLinesFromFile(String fileName) {
    List<String> lines = new ArrayList<>();
    try {
      File file = new File(fileName);
      if(!file.exists()) {
        log.error("No such file to read lines from: " + fileName);
        return lines;
      }

      Scanner scanner = new Scanner(file);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        if(!line.isEmpty())
          lines.add(line);
      }
      scanner.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    return lines;
  }
}
