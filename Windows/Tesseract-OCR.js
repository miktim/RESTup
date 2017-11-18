// RESTup. RESTful server
// 2013-2017, miktim@mail.ru
// Optical character recognizer Tesseract and langdata:
//    https://code.google.com/p/tesseract-ocr/
// Usage: cscript /nologo [selfdir]\OCRTesseract.js jobdir resdir [language]
//
try {
//
    var fso = new ActiveXObject("Scripting.FilesystemObject");
    var selfdir=fso.getParentFolderName(WScript.ScriptFullName);
    var wsh = WScript.CreateObject("WScript.Shell");
//
//    var servicename="OCRTesseract";
    var retcode=0;
    var jobdir = WScript.Arguments.item(0); // job file(s) folder (.JPG,.JPEG,...)
    var repdir = WScript.Arguments.item(1); // result file(s) folder (.TXT UTF-8)
    var language = "";
    if (WScript.Arguments.Count() > 2) language = ' -l '+ WScript.Arguments.item(2); 
//
// Call OCR. 
//
    var filescol = new Enumerator(fso.GetFolder(jobdir).Files);
    for (; !filescol.atEnd(); filescol.moveNext()) 
    {
        var file = filescol.item();
	WScript.stdErr.WriteLine(file.Name);	// restup service debug info
//	if (file.attributes & 16) continue;	// directory?
        retcode = wsh.run('"C:\\Program Files\\Tesseract-OCR\\tesseract.exe" '
             +' "'+jobdir+file.Name+'" '
             +' "'+repdir+file.Name+'"'+language
             ,0,true);
	if (retcode != 0 ) throw retcode;
    };
//
// exception?
//
} catch (err) {
    if (retcode == 0) retcode=1;	// script error
};
// timeout emulation
// WScript.Sleep(30000); 
//
WScript.Quit(retcode); 		// return exitcode 
