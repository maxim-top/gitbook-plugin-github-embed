import java.time.LocalDateTime
import java.io.PrintWriter
import java.io.File
import scala.io.Source
import java.io.BufferedInputStream
import java.io.FileInputStream
import scala.language.postfixOps
@main def init(name: String, url: String, branch: String, cacheDir: String) = {
   if (cacheDir == ""){
      var dir = "/tmp/%s-%d".format(name, System.currentTimeMillis())
      var cmd = "git clone %s %s -b %s".format(url, dir, branch)
      exec(cmd)
      formatVueFiles(dir)
      importCode(inputPath=dir,projectName=name)
      restoreVueFiles(dir)
   } else {
      if (new File(cacheDir).exists()){
         var cmd2 = "bash -c 'cd %s && git checkout %s && git pull --rebase'".format(cacheDir, branch)
         exec(cmd2)
      } else {
         var cmd2 = "git clone %s %s -b %s".format(url, cacheDir, branch)
         exec(cmd2)
      }
      if (isObjectiveC(cacheDir)) {
         var projectDir = findXcodeProjectDir(cacheDir)
         var dir = "/tmp/%s-%d".format(name, System.currentTimeMillis())
         var cpgFile = dir + "/cpg.bin.zip"
         var flags = "-fembed-bitcode -grecord-command-line -fno-inline-functions -fno-builtin"
         var cmd2 = "bash -c \"cd %s && xcodebuild OTHER_CFLAGS='%s' OTHER_CPLUSPLUSFLAGS='%s' OTHER_LDFLAGS='%s' clean build -quiet 1>/dev/null ; mkdir -p %s && llvm2cpg `find . -name '*.o'` --output=%s 1>/dev/null\"".format(projectDir, flags, flags, flags, dir, cpgFile)
         exec(cmd2)
         importCpg(inputPath=cpgFile,projectName=name)
      } else {
         formatVueFiles(cacheDir)
         importCode(inputPath=cacheDir,projectName=name)
         restoreVueFiles(cacheDir)
      }
   }
   close
   save
   exit
}

def findXcodeProjectDir(cacheDir: String) : String = {
   var files = recursiveListFiles(new File(cacheDir)).filter(_.getName.matches(".*\\.(xcworkspace|xcodeproj)"))
   if (files.length == 0){
      return cacheDir
   }
   var dir = ""
   files.foreach(file => {
      if (dir == "" || dir.length > file.getParent.length){
         dir = file.getParent
      }
   })
   return dir
}
def exec(cmd: String) = {
   println(cmd)
   scala.sys.process.Process(cmd).!
}

def isObjectiveC(dir: String): Boolean = {
   var files = recursiveListFiles(new File(dir))
   var ocFileCount = files.filter(_.getName.matches(".*\\.m")).length
   if (ocFileCount <= 0) {
      return false
   } else{
      var cFileCount = files.filter(_.getName.matches(".*\\.(c|cc)")).length
      var jsFileCount = files.filter(_.getName.matches(".*\\.(js|vue)")).length
      var javaFileCount = files.filter(_.getName.matches(".*\\.java")).length
      if (ocFileCount > cFileCount && ocFileCount > jsFileCount && ocFileCount > javaFileCount) {
         return true
      }else{
         return false
      }
   }
}
def recursiveListFiles(f: File): Array[File] = {
  val these = f.listFiles
  these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
}

def formatVueFiles(dir: String) = {
   val files = recursiveListFiles(new File(dir)).filter(_.getName.matches(".*\\.vue"))
   for(file <- files){
        val bis = new BufferedInputStream(new FileInputStream(file))
        val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
        var string = new String(bArray, "Utf-8");
        string = string.replaceAll("<template>", "<!--lanying-template>").replaceAll("</template>","</lanying-template-->")
        new PrintWriter(file) { write(string); close }
   }
}

def restoreVueFiles(dir: String) = {
   val files = recursiveListFiles(new File(dir)).filter(_.getName.matches(".*\\.vue"))
   for(file <- files){
        val bis = new BufferedInputStream(new FileInputStream(file))
        val bArray = Stream.continually(bis.read).takeWhile(-1 !=).map(_.toByte).toArray
        var string = new String(bArray, "Utf-8");
        string = string.replaceAll("<!--lanying-template>","<template>").replaceAll("</lanying-template-->", "</template>")
        new PrintWriter(file) { write(string); close }
   }
}