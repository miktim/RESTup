// RESTup. RESTful сервис консольных приложений
// 2013-2015, miktim@mail.ru
// Преобразование офисных (OpenOffice, LibreOffice, MS Office) документов
// Необходим LibreOffice 4.2, установленный по-умолчанию:
//    http://ru.libreoffice.org/
// см. также: http://www.commandlinefu.com/commands/view/11692/commandline-document-conversion-with-libreoffice
// запуск: cscript /nologo [selfdir]\Office2x.js jobdir resdir [PDF | HTML| OOXML | MSO97]
//
//
try {
//
    var fso = new ActiveXObject("Scripting.FilesystemObject");
    var selfdir=fso.getParentFolderName(WScript.ScriptFullName);
    var wsh = WScript.CreateObject("WScript.Shell");
//
    var servicename="Office2";
    var convertTo = "pdf"; // по-умолчанию к Adobe PDF
    var retcode=0;
    var jobdir = WScript.Arguments.item(0); // каталог файлов задания
    var repdir = WScript.Arguments.item(1); // каталог результатов
//  каталог вывода для Win soffice должен быть указан без завершающего бэкслэша!
    var repdir = repdir.substring(0, repdir.length-1); 
    if (WScript.Arguments.length > 2) convertTo = WScript.Arguments.item(2); 
    servicename = servicename + convertTo;
// WScript.stdErr.WriteLine(servicename);  // restup debug info
//
// преобразуем офисные документы к PDF, HTML, MSO97, MSO2007 (OOXML)
//
// шаблоны расширений файлов (документы, таблицы, презентации)
// и расшироения результирующих файлов при преобразовании к OOXML
// OOXML
    var jext = new Array(/.HTML$|.TXT$|.DOC$|.ODT$/, /.XLS$|.ODS$/, /.PPT$|.ODP$/);
    var rext = new Array("docx","xlsx", "pptx", "");
// MSO97
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
// подберем соответствующее расширение для преобразованного файла
	    for (; i < jext.length && filename.search(jext[i])==-1; i++) ;
	    convertTo = rext[i];
	};
	WScript.stdErr.WriteLine(convertTo);	// restup service debug info
// WScript.Echo(convertTo+" "+jobdir+" "+repdir);
        retcode = wsh.run('"C:\\Program Files\\LibreOffice 4\\program\\soffice.exe" --headless --convert-to '
             +convertTo+' --outdir '+repdir+' "'+jobdir+file.Name+'"',0,true);
	if (retcode != 0) throw retcode;
    };
    retcode=0;
//
// обработка ошибок
//
} catch (err) {
    if (retcode == 0) retcode=1;	// ошибка скрипта
};
// эмуляция таймаута
// WScript.Sleep(30000); 
//
WScript.Quit(retcode); 	// возвращаем код завершения 
