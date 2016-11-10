CREATE OR REPLACE PACKAGE  "RESTUP_AGENT" as
-- RESTup RESTful сервер консольных приложений PL/SQL API
-- 2015.09.22 переработан интерфейс, добавлена MIT лицензия
-- 2014.12.05 is вместо as в pipelined функциях
-- 2014.10.27 resfile_get, base642blob 
--            Special thanks to AScheffer (Oracle Community)
-- 2014.06.15
/*    
  Copyright (c) 2014-2015 
  miktim@mail.ru, 
  Петрозаводский государственный университет. РЦНИТ 
  (http://www.petrsu.ru/Structure/rcnit.html).

  Данная лицензия разрешает лицам, получившим копию данного программного
  обеспечения и сопутствующей документации (в дальнейшем именуемыми
  «Программное Обеспечение»), безвозмездно использовать Программное
  Обеспечение без ограничений, включая неограниченное право на использование,
  копирование, изменение, добавление, публикацию, распространение,
  сублицензирование и/или продажу копий Программного Обеспечения, а также
  лицам, которым предоставляется данное Программное Обеспечение, при
  соблюдении следующих условий:
  Указанное выше уведомление об авторском праве и данные условия должны быть
  включены во все копии или значимые части данного Программного Обеспечения.
  ДАННОЕ ПРОГРАММНОЕ ОБЕСПЕЧЕНИЕ ПРЕДОСТАВЛЯЕТСЯ «КАК ЕСТЬ», БЕЗ КАКИХ-ЛИБО
  ГАРАНТИЙ, ЯВНО ВЫРАЖЕННЫХ ИЛИ ПОДРАЗУМЕВАЕМЫХ, ВКЛЮЧАЯ ГАРАНТИИ ТОВАРНОЙ
  ПРИГОДНОСТИ, СООТВЕТСТВИЯ ПО ЕГО КОНКРЕТНОМУ НАЗНАЧЕНИЮ И ОТСУТСТВИЯ
  НАРУШЕНИЙ, НО НЕ ОГРАНИЧИВАЯСЬ ИМИ. НИ В КАКОМ СЛУЧАЕ АВТОРЫ ИЛИ
  ПРАВООБЛАДАТЕЛИ НЕ НЕСУТ ОТВЕТСТВЕННОСТИ ПО КАКИМ-ЛИБО ИСКАМ, ЗА УЩЕРБ ИЛИ
  ПО ИНЫМ ТРЕБОВАНИЯМ, В ТОМ ЧИСЛЕ, ПРИ ДЕЙСТВИИ КОНТРАКТА, ДЕЛИКТЕ ИЛИ ИНОЙ
  СИТУАЦИИ, ВОЗНИКШИМ ИЗ-ЗА ИСПОЛЬЗОВАНИЯ ПРОГРАММНОГО ОБЕСПЕЧЕНИЯ ИЛИ ИНЫХ
  ДЕЙСТВИЙ С ПРОГРАММНЫМ ОБЕСПЕЧЕНИЕМ.
*/
version constant varchar2(16) := '3.50609';
http_error exception;
pragma exception_init(http_error,-20268);
ora_http_error exception;
pragma exception_init(ora_http_error,-29273); -- Oracle HTTP request failed error (request timeout)
-- Для контроля исполнения можно использовать
-- UTL_HTTP.SET_RESPONSE_ERROR_CHECK(true):
-- разрешает exception в случае ошибки REST запроса (UTL_HTTP нет в Oracle-XE)
-- или локальную процедуру с подобным действием:
procedure set_response_error_check(p_val boolean);
-- apex_web_service.g_status_code : возвращает код состояния последнего REST запроса
-- 200,201,204  успешное завершение
default_url constant varchar2(512) := 'http://172.20.2.92:8080/restup/';
subType tp_urlencoded is varchar2(512); -- URI (urlEncoded)
-- описатель сервиса
Type tp_svc_rec is record
( uri tp_urlencoded        -- URI сервиса
, name varchar2(256)       -- имя сервиса
, fileexts varchar2(256)   -- расширения файлов, поддерживаемые сервисом
, jobmaxsize number        -- максимальный размер задания (байт)
, abstract varchar2(4000)  -- аннотация сервиса
);
Type tp_svcs_tbl is table of tp_svc_rec;
-- описатель файла результата исполнения задания
Type tp_file_rec is record
( uri tp_urlencoded        -- URI файла
, filename varchar2(256)   -- имя.расширение файла или имя/ = подкаталог
, filesize number          -- размер в байтах
, content BLOB             -- бинарное содержимое
);
Type tp_files_tbl is table of tp_file_rec;
-- SERVICES_TBL возвращает список сервисов
function services_tbl
( p_server varchar2 := default_url
) return tp_svcs_tbl pipelined;
-- SERVICE_URI вернуть uri сервиса
function service_get
( p_service varchar2       -- имя сервиса
, p_server varchar2 := default_url
) return tp_urlencoded;
-- JOB_CREATE создать задание сервису, вернуть URI задания
function job_create
( p_serviceuri tp_urlencoded
) return tp_urlencoded;
-- JOB_DELETE удалить задание и связанные файлы
procedure job_delete
( p_joburi tp_urlencoded
);
-- JOBFILE_PUT передать файл задания 
-- (количество файлов ограничено суммарным размером : jobquota)
procedure jobfile_put
( p_joburi tp_urlencoded
, p_filename varchar2
, p_content BLOB
);
-- JOB_EXECUTE стартовать задание, ждать исполнения, вернуть URI файлов-результатов
function job_execute
( p_joburi tp_urlencoded
, p_jobparams varchar2 := ''  -- параметры задания (зависят от сервиса)
)return tp_urlencoded;
-- RESFILES_TBL вернуть таблицу файлов-результатов (без содержимого файлов!)
function resfiles_tbl
( p_resuri tp_urlencoded
) return tp_files_tbl pipelined;     
-- RESFILE_GET возвращает содержимое файла-результата задания
function resfile_get
( p_fileuri tp_urlencoded
) return BLOB;
-- SERVUCE_EXECUTE выполнить задание с одиночным файлом или без файлов задания
-- Возвращает коллекцию файлов-результатов задания, включая бинарное содержимое
function service_execute
( p_service varchar2            -- имя сервиса
, p_filename varchar2 := null   -- сервис может только возвращать файлы в соответствии с параметром
, p_content BLOB := null
, p_jobparams varchar2 := null  -- параметры задания (зависят от сервиса)
, p_server varchar2 := default_url
) return tp_files_tbl;          -- коллекция файлов

end "RESTUP_AGENT";
/
CREATE OR REPLACE PACKAGE BODY  "RESTUP_AGENT" is
check_error boolean := false;
last_response CLOB;
-- 
procedure set_response_error_check(p_val boolean)
as
begin
  check_error:=p_val;
end;
-- проверить код состояния, инициировать exception
procedure check_response_error 
as
l_errmsg varchar2(500);
begin
  if check_error and apex_web_service.g_status_code not in ('200','201','204') then 
    l_errmsg:=regexp_replace(last_response,'<html><body><h1>([^>]+)</h1>([^$]+)$','\1');
    if l_errmsg is null then l_errmsg:=apex_web_service.g_status_code; end if;
    raise_application_error(-20268, 'HTTP error '||l_errmsg);
  end if;
end;
-- получить значение параметра заголовка по имени
Function pvalue_by_name(p_name varchar2) return varchar2
as
begin
for i in 1..apex_web_service.g_headers.count loop
  if lower(apex_web_service.g_headers(i).name)=lower(p_name) 
    then return apex_web_service.g_headers(i).value; end if;
end loop; 
return '';
end;

-- JOB_CREATE создать задание сервису, вернуть URI задания (может быть переадресован!)
Function job_create(p_serviceuri tp_urlencoded) return tp_urlencoded
as
begin
last_response := apex_web_service.make_rest_request(
  p_url => p_serviceuri --utl_url.escape(l_uri,false,'UTF-8')
 ,p_http_method => 'POST'    
);
check_response_error;
return pvalue_by_name('Location'); 
end;

-- JOB_EXECUTE запустить задание, ждать завершения, вернуть URI результатов
Function job_execute(p_joburi tp_urlencoded, p_jobparams varchar2:='') return tp_urlencoded
as
  l_body varchar2(1024);
begin
if p_jobparams is not null then
  apex_web_service.g_request_headers(1).name := 'Content-Type';
  apex_web_service.g_request_headers(1).value := 'text;charset=utf-8';
  l_body := p_jobparams;
end if;
last_response := apex_web_service.make_rest_request
    ( p_url => p_joburi
    , p_http_method => 'POST'
    , p_body => convert(l_body,'UTF8')
--    , p_transfer_timeout => 5  -- default 180 sec
    );
check_response_error;
return pvalue_by_name('Location');
end;

-- RESFILES_TBL получить таблицу файлов-результатов (БЕЗ СОДЕРЖИМОГО файлов!)
Function resfiles_tbl(p_resuri tp_urlencoded) return tp_files_tbl pipelined
is
l_uri varchar2(512);
l_nullblob BLOB;
begin
  apex_web_service.g_request_headers(1).name := 'Accept';
  apex_web_service.g_request_headers(1).value := 'text/xml';
    
  last_response := apex_web_service.make_rest_request
    ( p_url => utl_url.escape(p_resuri,false,'UTF-8')
    , p_http_method => 'GET'    
    );
  check_response_error;
  l_uri := pvalue_by_name('Content-Location');
  for sl in
    (select l_uri as URI
      , xtab.FILENAME
      , xtab.FILESIZE
      , l_nullblob as CONTENT
    from XMLTable('/restup_out/file' passing xmltype(last_response)
            COLUMNS 
                    "FILENAME" PATH 'name',
                    "FILESIZE" NUMBER PATH 'size'
     ) xtab)
  loop
    sl.uri:= l_uri||utl_url.escape(sl.filename,true,'UTF-8');
    pipe row (sl);
  end loop;
  return;
end;

-- JOB_DELETE удалить задание
Procedure job_delete(p_joburi tp_urlencoded)
as
l_uri tp_urlencoded;    
begin
l_uri := regexp_replace(p_joburi,'([^/]+//[^/]+/[^/]+/[^/]+/[^/]+/).*','\1',1,1);
last_response := apex_web_service.make_rest_request
    ( p_url => l_uri --||'in/'
    , p_http_method => 'DELETE'    
    );
end;

-- RESFILE_GET получить файл-результат
Function resfile_get(p_fileuri tp_urlencoded) return BLOB
as
begin
  return httpuritype( p_fileuri ).getblob();
end;

-- JOBFILE_PUT передать файл задания
Procedure jobfile_put(p_joburi tp_urlencoded, p_filename varchar2, p_content BLOB)
as
begin
  apex_web_service.g_request_headers(1).name := 'Content-Type';
  apex_web_service.g_request_headers(1).value := 'application/octet-stream';
  last_response:=apex_web_service.make_rest_request
    ( p_url => p_joburi || utl_url.escape(p_filename,false,'UTF-8')
    , p_http_method => 'PUT'
    , p_body_blob => p_content  
    );
  check_response_error;
end;

-- SERVICES_TBL - получить список сервисов
Function services_tbl
( p_server varchar2 := default_url
) return tp_svcs_tbl pipelined
is
begin
  apex_web_service.g_request_headers(1).name := 'Accept';
  apex_web_service.g_request_headers(1).value := 'text/xml';
  last_response := apex_web_service.make_rest_request(
    p_url => p_server
   ,p_http_method => 'GET'
  );
  check_response_error;
  for sl in
  (select xtab.* 
    from XMLTable('/restup/service' passing xmltype(last_response)
            COLUMNS "URI" PATH 'uri'
                  , "NAME" PATH 'name'
                  , "FILEEXTS" PATH 'fileExts'
                  , "JOBQUOTA" NUMBER PATH 'jobQuota'
                  , "ABSTRACT" PATH 'abstract'
     ) xtab)
  loop
    pipe row (sl);
  end loop;
  return;
end;

-- SERVICE_GET вернуть uri сервиса по его имени
function service_get
( p_service varchar2       -- имя сервиса
, p_server varchar2 := default_url
) return tp_urlencoded  
is
  l_uri tp_urlencoded;
begin
  select uri into l_uri from table(services_tbl(p_server))
    where upper(name) = upper(p_service);
  return l_uri;
end;

-- SERVICE_EXECUTE
-- выполнить сервис с одиночным файлом задания или без файлов задания
-- вернуть таблицу файлов-результатов с бинарным содержимым
Function service_execute
( p_service varchar2
, p_filename varchar2 := null
, p_content  BLOB := null
, p_jobparams varchar2 := null
, p_server varchar2 := default_url
) return tp_files_tbl
as
l_job tp_urlencoded;
l_uri tp_urlencoded;
l_resfiles tp_files_tbl:=tp_files_tbl();
begin
  l_job := job_create(service_get(p_service, p_server));
  check_response_error;
  if p_filename is not null then 
      jobfile_put(l_job, p_filename, p_content);
      check_response_error;
  end if;
  l_uri := job_execute(l_job, p_jobparams);
  check_response_error;
  select * bulk collect into l_resfiles from table(resfiles_tbl(l_uri));
  check_response_error;
  for i in 1..l_resfiles.count loop
      l_resfiles(i).content:=resfile_get(l_resfiles(i).uri);
      check_response_error;
  end loop;
  job_delete(l_job);
  return l_resfiles;
/*
exception
  when http_error or ora_http_error then
  if l_job is not null then job_delete(l_job); end if;
  check_error := l_check_error;
  raise;
*/
end;
end "RESTUP_AGENT";
/

