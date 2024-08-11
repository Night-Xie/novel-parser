package src;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.nio.file.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NovelToHTML {

    public String filepath;
    public String indexOutputDirectory;
    public String novelOutputDirectory;

    NovelToHTML(String[] args) {
        if(args.length > 3) {
            StringBuilder flags = new StringBuilder();
            for (int i = 3; i < args.length; i++) {
                flags.append(args[i]);
            }
            if (flags.toString().contains("-a")) ARABIC_CHAPTER_NUMBERS_ONLY_FLAG = true;

            this.filepath = args[0];
            this.novelOutputDirectory = args[1];
            this.indexOutputDirectory = args[2];
        }
        else {
            System.out.println("Incorrect number of arguments, " +
                    "try : java NovelToHTML.java originalNovelFilepath novelOutputDirectory indexOutputDirectory");
        }
    }

    private static final StringBuilder tocLinks = new StringBuilder();
    private static final StringBuilder novelSummary = new StringBuilder();

    public static File chapterTemplate = new File("resources/chapter_template.html");
    public static File tocTemplate = new File("resources/toc_template.html");
    public static File indexTemplate = new File("resources/index.html");
    public static File cssTemplate = new File("resources/styles.css");
    public static File jsTemplate = new File("resources/scripts.js");

    public static final Charset charset = StandardCharsets.UTF_8;

    Boolean ARABIC_CHAPTER_NUMBERS_ONLY_FLAG = false;

    public static void replace(Path path, String regex, String replacement) throws IOException {
        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(regex, Matcher.quoteReplacement(replacement));
        Files.write(path, content.getBytes(charset));
    }

    public void convert() throws IOException {

        Pattern chapterSplitter;

        if(ARABIC_CHAPTER_NUMBERS_ONLY_FLAG) {
            chapterSplitter = Pattern.compile("^第[0-9]*章.*");
        }
        else chapterSplitter = Pattern.compile("^第[0-9一二三四五六七八九十百零]*章.*");

        File novel = new File(filepath);
        if(!novel.exists()) throw new FileNotFoundException("Input file cannot be found. Check file path?");
        if(!novel.canRead()) throw new FileNotFoundException("Input file cannot be read.");

        Scanner reader = new Scanner(novel).useDelimiter("\r\n");
        String nextLine = reader.nextLine();

        // get summary for TOC
        while(reader.hasNextLine()) {
            if(Pattern.matches(chapterSplitter.pattern(), nextLine)) break;
            else {
                novelSummary.append("<p>");
                novelSummary.append(nextLine);
                novelSummary.append("</p>");
            }
            nextLine = reader.nextLine();
        }

        // generate chapters
        int chapterNumber = 1;
        while(reader.hasNextLine()) {
            //System.out.print("processing... chapter " + chapterNumber);
            String chapterFileName = "chapter_" + chapterNumber + ".html";

            //use chapter_template.html to create new mock files with filename chapter_x.html in output dir
            File chapter = new File(novelOutputDirectory + "/" + chapterFileName);
            if(chapter.exists()) chapter.delete();
            Files.copy(chapterTemplate.toPath(), chapter.toPath());

            //replace _TITLE with chapter title + add to array list
            replace(chapter.toPath(), "_TITLE", nextLine);
            String chapterLink = "<a href=\"" + chapterFileName + "\">"+ nextLine +"</a><br>\n";
            tocLinks.append(chapterLink);

            //replace _BODY with chapter contents (from chapter title to next chapter title)
            StringBuilder body = new StringBuilder();
            //body.append("<h2>").append(nextLine).append("</h2>");
            while(reader.hasNextLine()) {
                nextLine = reader.nextLine().strip();
                //if (nextLine.contains("第" + (chapterNumber + 1) + "章")) break;
                if (Pattern.matches(chapterSplitter.pattern(), nextLine)) {
                    break;
                }
                if(!nextLine.isBlank()) {
                    body.append("<p>");
                }
                body.append(nextLine);
                if(!nextLine.isBlank()) {
                    body.append("</p>");
                }
                body.append("\n");
            }
            replace(chapter.toPath(), "_BODY", body.toString());

            //replace _TOC with path (toc.html)
            replace(chapter.toPath(), "_TOC", "toc.html");

            //replace _PREV with filename of last chapter (if not chapter 1)
            if(chapterNumber != 1) {
                replace(chapter.toPath(), "_PREV", "chapter_" + (chapterNumber-1) + ".html");
            } else replace(chapter.toPath(), "<a href=\"_PREV\">上一章</a>", "");

            //replace _NEXT with filename of next chapter (if not last chapter)
            if(reader.hasNextLine()) {
                replace(chapter.toPath(), "_NEXT", "chapter_" + (chapterNumber+1) + ".html");
                chapterNumber++;
            } else replace(chapter.toPath(), "<a href=\"_NEXT\">下一章</a>\n", "没有了");

            //System.out.print(" ... done.\n");
        }
        reader.close();

        //System.out.print(" ... generating TOC");
        // generate TOC
        File toc = new File(novelOutputDirectory + "/toc.html");
        if(toc.exists()) toc.delete();
        Files.copy(tocTemplate.toPath(), toc.toPath());
        replace(toc.toPath(), "_TITLE", "目录");
        replace(toc.toPath(), "_SUMMARY", novelSummary.toString());
        replace(toc.toPath(), "_TOC", tocLinks.toString());
        // System.out.println(" ... done");

        // System.out.print(" ... cloning index");
        // create index.html
        File index = new File(indexOutputDirectory + "/index.html");
        if(index.exists()) index.delete();
        Files.copy(indexTemplate.toPath(), index.toPath());
        //System.out.println(" ...  done");

        // copy css
        File css = new File(novelOutputDirectory + "/styles.css");
        if(css.exists()) css.delete();
        Files.copy(cssTemplate.toPath(), css.toPath());
        css = new File(indexOutputDirectory + "/styles.css");
        if(css.exists()) css.delete();
        Files.copy(cssTemplate.toPath(), css.toPath());

        // copy js
        File js = new File(novelOutputDirectory + "/scripts.js");
        if(js.exists()) js.delete();
        Files.copy(jsTemplate.toPath(), js.toPath());
        js = new File(indexOutputDirectory + "/scripts.js");
        if(js.exists()) js.delete();
        Files.copy(jsTemplate.toPath(), js.toPath());

    }

    public static void main(String[] args) {
        try {
            NovelToHTML updater = new NovelToHTML(args);
            updater.convert();
            System.out.println("Conversion completed.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
