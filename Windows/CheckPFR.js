// RESTup. RESTful сервис консольных приложений
// 2013-2015, miktim@mail.ru
// Программа форматно-логического контроля обменных файлов ПФР checkPFR:
//   http://www.pfrf.ru/branches/bashkortostan/info/~Strahovatelyam/1423
// для распаковки zip-архивов используется 7-zip:
//   http://www.7-zip.org/
// запуск: cscript /nologo [selfdir]\CheckPFR.js jobdir resdir
//
try {
//
    var fso = new ActiveXObject("Scripting.FilesystemObject");
    var selfdir=fso.getParentFolderName(WScript.ScriptFullName);
    var wsh = WScript.CreateObject("WScript.Shell");
//
    var retcode=0;
    var jobdir = WScript.Arguments.item(0); // каталог файлов задания (.XML,.ZIP)
    var repdir = WScript.Arguments.item(1); // каталог результатов (.XML,.HTML)
    var fcnt=0; // исходных файлов
//
// проверить наличие и распаковать ZIP-архив(ы) в каталоге задания
//
    var filescol = new Enumerator(fso.GetFolder(jobdir).Files);
    var rez = /.ZIP$/;
    for (; !filescol.atEnd(); filescol.moveNext()) 
    {
        var file = filescol.item();
        if (file.Name.toUpperCase().search(rez) >= 0)
        {
// распаковать *.XML с помощью 7-zip и удалить ZIP-архив
            retcode = wsh.run("C:\\PROGRA~1\\7-Zip\\7z.exe e -o"+jobdir+" "+jobdir+"\\"+file.Name+" *.xml",0,true);
	    if (retcode != 0) throw retcode;
            file.Delete(true);
        };
    };
    var filescol = new Enumerator(fso.GetFolder(jobdir).Files);
    var filename = filescol.item().Name;  // любой XML-файл в каталоге задания
    for (fcnt=0; !filescol.atEnd(); filescol.moveNext()) fcnt++;
//  
// запуск checkPFR и ожидание завершения
//
    if (filename.length > 0)
        retcode = wsh.run("C:\\CheckPfr\\check.exe "+jobdir+"\\"+filename+" "+repdir+"\\",0,true);
    if (retcode != 0) throw retcode;
//
// обработка ошибок
//
} catch (err) {
    if (retcode == 0) retcode=1;	// 
};
// эмуляция таймаута
// WScript.Sleep(30000); 
//
WScript.Quit(retcode); 		// возвращаем код завершения 
