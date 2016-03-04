// RESTup. RESTful сервис консольных приложений
// 2013-2015, miktim@mail.ru
// ќптический распознаватель символов Tesseract:
//    https://code.google.com/p/tesseract-ocr/
// запуск: cscript /nologo [selfdir]\OCRTesseract.js jobdir resdir [params]
//
try {
//
    var fso = new ActiveXObject("Scripting.FilesystemObject");
    var selfdir=fso.getParentFolderName(WScript.ScriptFullName);
    var wsh = WScript.CreateObject("WScript.Shell");
//
//    var servicename="OCRTesseract";
    var retcode=0;
    var jobdir = WScript.Arguments.item(0); // папка файлов задани€ (.JPG,.JPEG,...)
    var repdir = WScript.Arguments.item(1); // папка результатов (.TXT UTF-8)
    var language = "rus";
    if (WScript.Arguments.Count() > 2) language = WScript.Arguments.item(2); 
//
// вызвать OCR дл€ файлов задани€. 
//
    var filescol = new Enumerator(fso.GetFolder(jobdir).Files);
    for (; !filescol.atEnd(); filescol.moveNext()) 
    {
        var file = filescol.item();
	WScript.stdErr.WriteLine(file.Name);	// restup service debug info
//	if (file.attributes & 16) continue;	// directory?
        retcode = wsh.run('"C:\\Program Files\\Tesseract-OCR\\tesseract.exe" '
             +' "'+jobdir+file.Name+'" '
             +' "'+repdir+file.Name+'" -l '+ language
             ,0,true);
	if (retcode != 0 ) throw retcode;
    };
//
// обработка ошибок
//
} catch (err) {
    if (retcode == 0) retcode=1;	// ошибка скрипта
};
// эмул€ци€ таймаута
// WScript.Sleep(30000); 
//
WScript.Quit(retcode); 		// возвращаем код завершени€ 
