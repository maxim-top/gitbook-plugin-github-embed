import java.time.LocalDateTime
import java.io.PrintWriter
import java.io.File
import scala.io.Source
import java.io.BufferedInputStream
import java.io.FileInputStream
import scala.language.postfixOps
import scala.util.control.Breaks._
@main def exec(repo: String) = {
   workspace.openProject(repo)
   for (ln <- Source.stdin.getLines) {
      var fields = ln.split(" ")
      if (fields(0) == "ExtractCode") {
         try {
            extractCode(fields(1),fields(2),fields(3),fields(4), fields(5))
         }catch{
            case error: Throwable =>
               println(error)
         }
         printf("ExtractCodeFinish")
      }
   }
}

def extractCode(className: String, functionName: String, maxLine: String, maxSnippetCount: String, cacheDir: String) = {
   var language = cpg.metaData.language.toList(0)
   var snippetCount = 0
   var maxSnippetCountInt = maxSnippetCount.toInt
   if (language ==  "JAVASRC") {
      var rule = ".*%s[.:->]*%s[(:]+.*".format(className, functionName)
      cpg.call.methodFullName(rule).foreach(r => {
         var filename = r.inAst.isMethod.toList(0).filename
         var lineNumber = r.lineNumber
         var repoPath = project.projectFile.inputPath
         var method = r.inAst.isMethod.toList(0)
         var lineStart = method.lineNumber
         var lineEnd = method.lineNumberEnd
         var code = getFileLines(filename, lineStart, lineEnd, lineNumber, maxLine)
         snippetCount += 1
         if (snippetCount <= maxSnippetCountInt) printResult(filename, lineNumber, code, repoPath)
      })
      var methodRule = ".*%s\\.<init>.*".format(className)
      var codeRule = "new .*%s.*\\{.*%s.*".format(className, functionName)
      cpg.call.methodFullName(methodRule).code(codeRule).foreach(r => {
         var filename = r.inAst.isMethod.toList(0).filename
         var blockLineNumber = r.lineNumber
         var repoPath = project.projectFile.inputPath
         var codeLineCount = r.code.count(_ == '\n') + 1
         var blockCode = getFileLines(filename, blockLineNumber, Some(blockLineNumber.get + codeLineCount + 100), blockLineNumber, "100000000")
         var (code, lineNumber) = getFunctionCode(functionName, blockCode, blockLineNumber, maxLine)
         if (code != "") {
            snippetCount += 1
            if (snippetCount <= maxSnippetCountInt) printResult(filename, lineNumber, code, repoPath)
         }
      })
      var childClassRule = ".*%s$".format(className)
      cpg.typeDecl.filter(_.inheritsFromTypeFullName.filter(_.matches(childClassRule)).size > 0).ast.isMethod.name(functionName).foreach(
         r => {
            var filename = r.filename
            var lineNumber = r.lineNumber
            if (lineNumber.isDefined){
               lineNumber = Some(lineNumber.get + 1)
            }
            var lineStart = lineNumber
            var lineEnd = r.lineNumberEnd
            if (lineEnd.isDefined){
               lineEnd = Some(lineEnd.get + 1)
            }
            var repoPath = project.projectFile.inputPath
            var code = getFileLines(filename, lineStart, lineEnd, lineNumber, maxLine)
            if (code != "") {
               snippetCount += 1
               if (snippetCount <= maxSnippetCountInt) printResult(filename, lineNumber, code, repoPath)
            }
         }
      )
   } else if (language == "NEWC") {
      if (className == ""){//C++ global function
         var rule = "^%s$".format(functionName)
         cpg.call.methodFullName(rule).foreach(r => {
            var filename = r.inAst.isMethod.toList(0).filename
            var lineNumber = r.lineNumber
            var repoPath = project.projectFile.inputPath
            var method = r.inAst.isMethod.toList(0)
            var lineStart = method.lineNumber
            var lineEnd = method.lineNumberEnd
            var code = getFileLines(filename, lineStart, lineEnd, lineNumber, maxLine)
            snippetCount += 1
            if (snippetCount <= maxSnippetCountInt) printResult(filename, lineNumber, code, repoPath)
         })
      } else {//C++ function with class
         var rule = ".*[.:->]+%s$".format(functionName)
         cpg.call.methodFullName(rule).foreach(r => {
            var myClassName = findClass(r.astChildren.toList(0).astChildren.toList(0))
            if (myClassName == className || myClassName == "ANY") {
               var filename = r.inAst.isMethod.toList(0).filename
               var lineNumber = r.lineNumber
               var repoPath = project.projectFile.inputPath
               var method = r.inAst.isMethod.toList(0)
               var lineStart = lineNumber
               var lineEnd = None
               var code = getFileLines(filename, lineStart, lineEnd, lineNumber, maxLine)
               snippetCount += 1
               if (snippetCount <= maxSnippetCountInt) printResult(filename, lineNumber, code, repoPath)
            }
         })
      }
   } else if (language == "LLVM") { // Objective-C ...
      var rule = "^%s[:]*$".format(functionName)
      cpg.call.code(rule).foreach(r => {
         var filename = r.inAst.isMethod.toList(0).filename
         var lineNumber = r.lineNumber
         var repoPath = cacheDir
         var method = r.inAst.isMethod.toList(0)
         var lineStart = method.lineNumber
         var lineEnd = method.lineNumberEnd
         if (lineNumber.isDefined) {
            var code = getFileLines(filename, lineStart, lineEnd, lineNumber, maxLine)
            snippetCount += 1
            if (snippetCount <= maxSnippetCountInt) printResult(filename, lineNumber, code, repoPath)
         }
      })
      cpg.method.name(rule).foreach(r => {
         var filename = r.filename
         var lineNumber = r.lineNumber
         var repoPath = cacheDir
         if (lineNumber.isDefined) {
            var code = getFunctionCodeByLine(filename, lineNumber, maxLine)
            snippetCount += 1
            if (snippetCount <= maxSnippetCountInt) printResult(filename, lineNumber, code, repoPath)
         }
      })
   } else { // javascript
      var rule = "[^\\n=]*%s[.:->]*%s[(:]+.*".format(className, functionName)
      cpg.call.code(rule).foreach(r => {
          var filename = r.inAst.isMethod.toList(0).filename
          var lineNumber = r.lineNumber
          var repoPath = project.projectFile.inputPath
          var code = r.inAst.isBlock.toList(0).code
          //var startLineNumber = r.inAst.isBlock.toList(0).lineNumber.get
         snippetCount += 1
         if (snippetCount <= maxSnippetCountInt) printResult(filename, lineNumber, code, repoPath)
      })
   }
}

def findClass(node: AstNode) : String = {
   //printf("IN:%s:%s\n", node.toString, node.code)
   if (node.isCall){
      var call =  node.ast.isCall.toList(0)
      if (call.name == "<operator>.indirectFieldAccess"){
         var children = node.astChildren.toList
         var className = findClass(children(0))
         var function = children(1).ast.isFieldIdentifier.toList(0).canonicalName
         if (className == "ANY") className = ""
         //printf("Search:%s|%s\n", className, function)
         var methods = cpg.method.fullName(".*%s.%s$".format(className, function)).toList
         if (methods.length > 0) {
            var result = methods(0).signature.replaceAll("([^ ]*) .*","$1")
            //printf("RETURN:%s\n", result)
            return result
         }else{
            return "<CLASS_NOT_FOUND>"
         }
      }else{
         var children = node.astChildren.toList
         var result = findClass(children(0))
         //printf("RETURN:%s\n", result)
         return result
      }
   } else if (node.isIdentifier){
      var result = findPointerClass(node.ast.isIdentifier.toList(0).typeFullName)
      //printf("RETURN:%s\n", result)
      return result
   }
   return "<CLASS_NOT_FOUND>"
}

def findPointerClass(classStr: String) : String = {
   var types = cpg.typeDecl.name(classStr).toList
   if (types.length > 0) {
      if (types(0).code.matches("typedef std::shared_ptr<.*>[^\n]*")){
         var code = types(0).code
         return code.replaceAll("typedef std::shared_ptr<(.*)>.*","$1")
      }
      return types(0).name
   }
   return "<CLASS_NOT_FOUND>"
}
def printResult(filename: String, lineNumber: Option[Integer], code: String, repoPath: String) = {
   var relativeFilename = filename
   if(filename.startsWith(repoPath)){
      relativeFilename = filename.substring(repoPath.length()+1)
   }
   if (lineNumber.isDefined){
      var lineDelimiter = "__LANYING_CODE_SNAPPET_LINE_DELIMITER__"
      var fieldDelimiter = "__LANYING_CODE_SNAPPET_FIELD_DELIMITER__"
      printf("CodeSnippet%s%s%s%d%s%s%s%s\n",
         fieldDelimiter, relativeFilename,
         fieldDelimiter, lineNumber.get,
         fieldDelimiter, code.replaceAll("\n",lineDelimiter),
         fieldDelimiter, repoPath)
   }
}

def getFileLines(filename: String, lineStart: Option[Integer], lineEnd: Option[Integer], lineNumber: Option[Integer], maxLine: String): String = {
   var lineNumberInt = lineNumber.get
   var lineStartInt = lineNumberInt
   var lineEndInt = lineNumberInt
   var maxLineInt = maxLine.toInt
   if (lineStart.isDefined) {
      lineStartInt = lineStart.get
   }
   if (lineEnd.isDefined){
      lineEndInt = lineEnd.get
   }
   if (lineEndInt - lineStartInt + 1 > maxLineInt) {
      if (lineStartInt < lineNumberInt - maxLineInt / 2) {
         lineStartInt = lineNumberInt - maxLineInt / 2
      }
      if (lineEndInt > lineStartInt + maxLineInt - 1) {
         lineEndInt = lineStartInt + maxLineInt - 1
      }
   }
   val lines = Source.fromFile(filename).getLines()
   return lines.slice(lineStartInt-1,lineEndInt).mkString("\n")
}

def getFunctionCodeByLine(filename: String, lineNumber: Option[Integer], maxLine: String) : String = {
   var code = getFileLines(filename, lineNumber, Some(lineNumber.get + 100000000), lineNumber, maxLine)
   var lines = code.split("\n")
   var leftCurlyBraceCnt = 0
   var rightCurlyBraceCnt = 0
   var retCode = ""
   breakable{
      for (line <- lines){
         retCode += line + "\n"
         leftCurlyBraceCnt += line.count(_ == '{')
         rightCurlyBraceCnt += line.count(_ == '}')
         if (leftCurlyBraceCnt > 0 && rightCurlyBraceCnt >= leftCurlyBraceCnt){
            break
         }
      }
   }
   return retCode
}

def getFunctionCode(functionName: String, blockCode: String, blockLineNumber: Option[Integer], maxLine: String): (String, Option[Integer]) = {
   var maxLineInt = maxLine.toInt
   var lines = blockCode.split("\n")
   var code = ""
   var lineNumber = blockLineNumber.get
   var isFound = false
   var finish = false
   var lineCount = 0
   var codeCount = 0
   var rule = ".*[ \t]+%s[ \t\r\n(]+.*".format(functionName)
   var leftCurlyBraceCnt = 0
   var rightCurlyBraceCnt = 0
   lines.foreach(line => {
      lineCount += 1
      if (!isFound && line.matches(rule)){
         lineNumber = lineNumber + lineCount-1
         code += line + "\n"
         codeCount += 1
         leftCurlyBraceCnt += line.count(_ == '{')
         rightCurlyBraceCnt += line.count(_ == '}')
         if (rightCurlyBraceCnt >= leftCurlyBraceCnt){
            finish = true
         }
         isFound = true
      } else if(isFound && !finish) {
         codeCount += 1
         if (codeCount <= maxLineInt) {
            code += line + "\n"
         }
         leftCurlyBraceCnt += line.count(_ == '{')
         rightCurlyBraceCnt += line.count(_ == '}')
         if (rightCurlyBraceCnt >= leftCurlyBraceCnt){
            finish = true
         }
      }
   })
   return (code, Some(lineNumber))
}