/**
 * 
 */
package br.com.sambatest.core.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.primefaces.model.UploadedFile;

import br.com.sambatest.core.model.Arquivo;

import com.amazonaws.services.s3.model.PutObjectResult;

/**
 * @author leandro.pereira
 *
 */
public interface ArquivoService {

	public void uploadFile(UploadedFile file) throws IOException;
	
	public PutObjectResult insertFile(File file);
	
	public List<Arquivo> findAll();
	
	public List<Arquivo> find(String key);
	
	public String getUrlFile(Arquivo arquivo);

	public void excluir(Arquivo selectedArquivo);
}
