import java.time.LocalDateTime
import java.io.PrintWriter
import java.io.File
import scala.io.Source
import java.io.BufferedInputStream
import java.io.FileInputStream
import scala.language.postfixOps
@main def exec(name: String, url: String, branch: String) = {
   var dir = "/tmp/%s-%d".format(name, System.currentTimeMillis())
   var cmd = "git clone %s %s -b %s".format(url, dir, branch)
   scala.sys.process.Process(cmd).!!
   formatVueFiles(dir)
   importCode(inputPath=dir,projectName=name)
   close
   save
   exit
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
        string = string.replaceAll("<template>", "<!--template>").replaceAll("</template>","</template-->")
        new PrintWriter(file) { write(string); close }
   }
}