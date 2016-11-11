declare
-- !!! SET CORRECT host URL below
  l_host varchar2(50) := 'http://localhost:8080/restup';
  l_uri varchar2(500);-- returned URI
  l_sblob BLOB; -- source file
  l_rblob BLOB; -- returned file
-- Returns random generated BLOB
function generateBLOB(p_size number) return BLOB
is
  l_blob BLOB;
begin
  dbms_lob.createtemporary(l_blob,true,dbms_lob.call);
  while dbms_lob.getlength(l_blob) < p_size loop
    dbms_lob.append(l_blob,utl_raw.cast_from_number(dbms_random.value()));
  end loop;
  dbms_lob.trim(l_blob, p_size);
  return l_blob;
end;
begin
-- get echo service URI, output http status code & URI 
  l_uri := restup_agent.service_get('echo', l_host);
  dbms_output.put_line(apex_web_service.g_status_code||' '||l_uri);
-- create job for echo service, output http status code &  URI for job files
  l_uri := restup_agent.job_create(l_uri);
  dbms_output.put_line(apex_web_service.g_status_code||' '||l_uri);
-- generate job file contents & transfer to server with different extensions
  l_sblob := generateBLOB(123456);
  restup_agent.jobfile_put(l_uri,'Test-file 1.bin',l_sblob);
  restup_agent.jobfile_put(l_uri,'Test-file 2.tmp',l_sblob);
-- execute the job with file mask as a job parameter
-- output http status code & result file(s) URI
  l_uri:=restup_agent.job_execute(l_uri,'*.tmp');
  dbms_output.put_line(apex_web_service.g_status_code||' '||l_uri);
-- list result files
  for t in (select * from table(restup_agent.resfiles_tbl(l_uri))) loop
    dbms_output.put_line(t.filename||' '||t.filesize);
-- get contents of result file
    l_rblob:=restup_agent.resfile_get(t.uri);
-- compare contents of the source & the result files
    dbms_output.put_line(apex_web_service.g_status_code||' '
      ||dbms_lob.compare(l_sblob,l_rblob));
-- free LOB
    if dbms_lob.istemporary(l_rblob)=1 
      then dbms_lob.freetemporary(l_rblob); end if;
  end loop;
-- delete job
  restup_agent.job_delete(l_uri);
-- free source LOB
  if dbms_lob.istemporary(l_sblob)=1
    then dbms_lob.freetemporary(l_sblob); end if;
end;
