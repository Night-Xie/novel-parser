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
    public String baseOutputDirectory;
    public String novelFolderName;
    public String novelOutputDirectory; // Derived from base + novel folder

NovelToHTML(String[] args) {
        if (args.length == 0 || args[0].equals("-h") || args[0].equals("--help")) {
            printUsageAndExit();
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i":
                    if (i + 1 < args.length) this.filepath = args[++i];
                    break;
                case "-o":
                    if (i + 1 < args.length) this.baseOutputDirectory = args[++i];
                    break;
                case "-n":
                    if (i + 1 < args.length) this.novelFolderName = args[++i];
                    break;
            }
        }

        if (this.filepath == null || this.baseOutputDirectory == null) {
            System.err.println("\n[Error] Missing required arguments (-i and -o).");
            printUsageAndExit();
        }

        // AUTO-DERIVE NOVEL NAME: If -n wasn't provided, use the text file's name
        if (this.novelFolderName == null) {
            File f = new File(this.filepath);
            String name = f.getName();
            int dotIndex = name.lastIndexOf('.');
            this.novelFolderName = (dotIndex > 0) ? name.substring(0, dotIndex) : name;
        }

        this.novelOutputDirectory = this.baseOutputDirectory + "/" + this.novelFolderName;
    }

    // AUTO-DETECT CHAPTER FORMAT
    private Pattern determineChapterPattern(File novel) throws IOException {
        // Broad pattern to catch BOTH formats initially
        Pattern generalPattern = Pattern.compile("^\\s*第\\s*([0-9]+|[一二三四五六七八九十百千万零]+)\\s*章.*");
        
        try (Scanner scanner = new Scanner(novel).useDelimiter("\r\n")) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Matcher m = generalPattern.matcher(line);
                if (m.matches()) {
                    String numberPart = m.group(1);
                    // If the first chapter uses 0-9, lock to Arabic. Otherwise, Chinese.
                    if (numberPart.matches("[0-9]+")) {
                        System.out.println("--> Auto-detected ARABIC chapter numbers.");
                        return Pattern.compile("^\\s*第\\s*[0-9]+\\s*章.*");
                    } else {
                        System.out.println("--> Auto-detected CHINESE chapter numbers.");
                        return Pattern.compile("^\\s*第\\s*[一二三四五六七八九十百千万零]+\\s*章.*");
                    }
                }
            }
        }
        // Fallback just in case
        return Pattern.compile("^\\s*第[0-9一二三四五六七八九十百零]*章.*");
    }

    private void printUsageAndExit() {
        System.out.println("\nUsage: java NovelToHTML -i <input.txt> -o <output_dir> -n <novel_folder> [-a]");
        System.out.println("\nRequired Arguments:");
        System.out.println("  -i   Path to the original novel text file.");
        System.out.println("  -o   Base output directory (where index.html will be generated).");
        System.out.println("  -n   Name of the folder to create for the novel's chapters.");
        System.out.println("\nOptional Arguments:");
        System.out.println("  -a   Flag to use Arabic chapter numbers only.");
        System.out.println("  -h   Show this help message.\n");
        System.exit(1); 
    }

    private static final StringBuilder tocLinks = new StringBuilder();
    private static final StringBuilder novelSummary = new StringBuilder();

    public static File chapterTemplate = new File("resources/chapter_template.html");
    public static File tocTemplate = new File("resources/toc_template.html");
    public static File indexTemplate = new File("resources/index.html");
    public static File cssTemplate = new File("resources/styles.css");
    public static File jsTemplate = new File("resources/scripts.js");

    public static final Charset charset = StandardCharsets.UTF_8;

    public static void replace(Path path, String regex, String replacement) throws IOException {
        String content = new String(Files.readAllBytes(path), charset);
        content = content.replaceAll(regex, Matcher.quoteReplacement(replacement));
        Files.write(path, content.getBytes(charset));
    }

    public void convert() throws IOException {

        File novel = new File(filepath);
        if(!novel.exists()) throw new FileNotFoundException("Input file cannot be found. Check file path?");
        if(!novel.canRead()) throw new FileNotFoundException("Input file cannot be read.");

        // Determine the regex dynamically based on the file content!
        Pattern chapterSplitter = determineChapterPattern(novel);


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

        (new File(novelOutputDirectory)).mkdir();

        // generate chapters
        int chapterNumber = 1;
        while(reader.hasNextLine()) {
            // \r brings the cursor to the start of the line, overwriting the previous number
            System.out.print("\rProcessing chapter " + chapterNumber + " ... ");
            
            String chapterFileName = "chapter_" + chapterNumber + ".html";

            // ... (keep the rest of your chapter generation code exactly the same) ...

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
            while(reader.hasNextLine()) {
                nextLine = reader.nextLine().strip();
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
            } else replace(chapter.toPath(), "<a href=\"_PREV\" id=\"prevLink\">上一章</a>", "");

            //replace _NEXT with filename of next chapter (if not last chapter)
            if(reader.hasNextLine()) {
                replace(chapter.toPath(), "_NEXT", "chapter_" + (chapterNumber+1) + ".html");
                chapterNumber++;
            } else replace(chapter.toPath(), "<a href=\"_NEXT\" id=\"nextLink\">下一章</a>", "<span style=\"padding: 10px;\">没有了</span>");
        }

        // Add this empty println so the final console output doesn't get overwritten
        System.out.println(); 
        reader.close();

        // generate TOC (Updated to show the Novel's name in the Title!)
        File toc = new File(novelOutputDirectory + "/toc.html");
        if(toc.exists()) toc.delete();
        Files.copy(tocTemplate.toPath(), toc.toPath());
        replace(toc.toPath(), "_TITLE", novelFolderName + " - 目录");
        replace(toc.toPath(), "_SUMMARY", novelSummary.toString());
        replace(toc.toPath(), "_TOC", tocLinks.toString());

        // --- CLEAN REWRITE OF INDEX.HTML ---
        File baseDir = new File(baseOutputDirectory);
        File[] subDirs = baseDir.listFiles(File::isDirectory);
        StringBuilder indexLinks = new StringBuilder();

        if (subDirs != null) {
            // Sort alphabetically so your library is always in order
            java.util.Arrays.sort(subDirs);
            
            for (File dir : subDirs) {
                // Only create a link if the folder actually contains a parsed novel
                if (new File(dir, "toc.html").exists()) {
                    String folderName = dir.getName();
                    indexLinks.append("<p><a href=\"")
                              .append(folderName)
                              .append("/toc.html\">")
                              .append(folderName)
                              .append("</a></p>\n        ");
                }
            }
        }

        // Read the clean skeleton template
        String indexContent = new String(Files.readAllBytes(indexTemplate.toPath()), charset);
        
        // Swap the placeholder with the freshly generated links
        indexContent = indexContent.replace("_LINKS_", indexLinks.toString());
        
        // Write the completely fresh index.html to the output directory
        File index = new File(baseOutputDirectory + "/index.html");
        Files.write(index.toPath(), indexContent.getBytes(charset));
        
        System.out.println("--> Successfully rebuilt index.html from scratch!");

        // copy css
        File css = new File(novelOutputDirectory + "/styles.css");
        if(css.exists()) css.delete();
        Files.copy(cssTemplate.toPath(), css.toPath());
        css = new File(baseOutputDirectory + "/styles.css");
        if(css.exists()) css.delete();
        Files.copy(cssTemplate.toPath(), css.toPath());

        // copy js
        File js = new File(novelOutputDirectory + "/scripts.js");
        if(js.exists()) js.delete();
        Files.copy(jsTemplate.toPath(), js.toPath());
        js = new File(baseOutputDirectory + "/scripts.js");
        if(js.exists()) js.delete();
        Files.copy(jsTemplate.toPath(), js.toPath());

        // copy PWA files (Service Worker, Manifest, and Icons) to the base directory
        // Update the "resources/" path below if your templates are stored in a different folder!
        String[] pwaFiles = {"sw.js", "manifest.json", "icon-192.png", "icon-512.png"};
        
        for (String fileName : pwaFiles) {
            File sourceFile = new File("resources/" + fileName); // <-- Adjust this folder name if needed
            File destFile = new File(baseOutputDirectory + "/" + fileName);
            
            try {
                if (sourceFile.exists()) {
                    if (destFile.exists()) destFile.delete();
                    Files.copy(sourceFile.toPath(), destFile.toPath());
                } else {
                    System.out.println("Warning: " + fileName + " not found in source folder. PWA might not install.");
                }
            } catch (IOException e) {
                System.out.println("Failed to copy PWA file: " + fileName);
            }
        }
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