// RESTup. RESTful server of console apps
// 2013-2017, miktim@mail.ru
// Convert office files (OpenOffice, LibreOffice, MS Office)
// Require LibreOffice 5.x, installed "by default": http://libreoffice.org/
// See also: http://www.commandlinefu.com/commands/view/11692/commandline-document-conversion-with-libreoffice
// Usage: cscript /nologo [selfdir\]Office2x.js jobdir resdir [PDF | HTML| OOXML | MSO97]
//        jobdir, resdir with trailing backslash
//
var sofficePath = '"C:\\Program Files\\LibreOffice 5\\program\\soffice.exe"';
try {
//
    var fso = new ActiveXObject("Scripting.FilesystemObject");
    var selfdir=fso.getParentFolderName(WScript.ScriptFullName);
    var wsh = WScript.CreateObject("WScript.Shell");
//
    var servicename="Office2";
    var convertTo = "pdf"; // by default convert to Adobe PDF
    var retcode=0;
    var jobdir = WScript.Arguments.item(0); // 
    var repdir = WScript.Arguments.item(1); // 
// The output directory for Win Soffice must be specified without the trailing backslash!
    var repdir = repdir.substring(0, repdir.length-1); 
    if (WScript.Arguments.length > 2) convertTo = WScript.Arguments.item(2); 
    servicename = servicename + convertTo;
// WScript.stdErr.WriteLine(servicename);  // restup debug info
//
// Convert Office Documents to PDF, HTML, MS Office97, MS Office2007 (OOXML)
//
// Source and converted file extension mapping tables
// to OOXML
    var jext = new Array(/.HTML$|.TXT$|.DOC$|.ODT$/, /.XLS$|.ODS$/, /.PPT$|.ODP$/);
    var rext = new Array("docx","xlsx", "pptx", "");
// to MSO97
    if (servicename.toUpperCase() == "OFFICE2MSO97") {
       var jext = new Array(/.HTML$|.TXT$|.DOCX$|.ODT$/, /.XLSX$|.ODS$/, /.PPTX$|.ODP$/);
       var rext = new Array("doc","xls", "ppt", "");
    }
    var filescol = new Enumerator(fso.GetFolder(jobdir).Files);
    for (; !filescol.atEnd(); filescol.moveNext()) 
    {
        var file = filescol.item();
	WScript.stdErr.WriteLine(file.Name);	// restup service debug info
//	if (file.attributes & 16) continue;	// directory?
        if (servicename.toUpperCase() == "OFFICE2OOXML"
		|| servicename.toUpperCase() == "OFFICE2MSO97") {
            var filename = file.Name.toUpperCase();
            var i=0;
// get appropriate extension for result file
	    for (; i < jext.length && filename.search(jext[i])==-1; i++) ;
	    convertTo = rext[i];
	};
	WScript.stdErr.WriteLine(convertTo);	// restup service debug info
// WScript.Echo(convertTo+" "+jobdir+" "+repdir);
        retcode = wsh.run(sofficePath + ' --headless --convert-to '
             +convertTo+' --outdir '+repdir+' "'+jobdir+file.Name+'"',0,true);
	if (retcode != 0) throw retcode;
    };
    retcode=0;
//
// exception
//
} catch (err) {
    if (retcode == 0) retcode=1;	// script error
};
// timeout emulation
// WScript.Sleep(30000); 
//
WScript.Quit(retcode); 	//  
