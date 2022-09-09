import java.time.LocalDateTime
import java.io.PrintWriter
import java.io.File
import scala.io.Source
import java.io.BufferedInputStream
import java.io.FileInputStream
import scala.language.postfixOps
@main def exec(repo: String) = {
   workspace.openProject(repo)
   for (ln <- Source.stdin.getLines) {
      var fields = ln.split(" ")
      if (fields(0) == "ExtractCode") {
         extractCode(fields(1),fields(2),fields(3),fields(4))
         printf("ExtractCodeFinish")
      }
   }
}

def extractCode(className: String, functionName: String, maxLine: String, maxSnippetCount: String) = {
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
               var lineStart = method.lineNumber
               var lineEnd = method.lineNumberEnd
               var code = getFileLines(filename, lineStart, lineEnd, lineNumber, maxLine)
               snippetCount += 1
               if (snippetCount <= maxSnippetCountInt) printResult(filename, lineNumber, code, repoPath)
            }
         })
      }
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
   var lineDelimiter = "__LANYING_CODE_SNAPPET_LINE_DELIMITER__"
   var fieldDelimiter = "__LANYING_CODE_SNAPPET_FIELD_DELIMITER__"
   printf("CodeSnippet%s%s%s%d%s%s%s%s\n",
      fieldDelimiter, relativeFilename,
      fieldDelimiter, lineNumber.get,
      fieldDelimiter, code.replaceAll("\n",lineDelimiter),
      fieldDelimiter, repoPath)
}

def getFileLines(filename: String, lineStart: Option[Integer], lineEnd: Option[Integer], lineNumber: Option[Integer], maxLine: String): String = {
   var lineNumberInt = lineNumber.get
   var lineStartInt = lineStart.get
   var lineEndInt = lineStartInt
   var maxLineInt = maxLine.toInt
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