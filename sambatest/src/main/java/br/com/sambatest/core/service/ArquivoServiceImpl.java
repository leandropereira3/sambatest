/**
 * 
 */
package br.com.sambatest.core.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.faces.context.FacesContext;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.primefaces.model.UploadedFile;
import org.springframework.stereotype.Service;

import br.com.sambatest.core.model.Arquivo;
import br.com.sambatest.core.util.ParametersUtil;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * @author leandro.pereira
 *
 */

@Service
public class ArquivoServiceImpl implements ArquivoService {
	static final String PARAM_BUCKET_NAME = "bucket"; 
	static final String PARAM_BUCKET_URL = "urlbucket";
	static final String PARAM_ACCESS_KEY = "accesskey";
	static final String PARAM_SECRET_KEY = "secretkey";
	static final String PARAM_ZENCODER_URL = "zencoderurl";
	static final String PARAM_ZENCODER_KEY = "zencoderkey";
	static final String PARAM_ZENCODER_GRANTEE = "zencodergrantee";	
	static final String READ_PERMISSION = "READ";
	static final Integer ONE_MB = 1000000;
	

	public void uploadFile(UploadedFile file) {		
		System.out.println("file" + file);
		File newFile = new File(getTempDir() + file.getFileName());

		//Cria um arquivo no diretorio temp com os dados do arquivo enviado
		try {
			InputStream inputStream = file.getInputstream();
			OutputStream outputStream = new FileOutputStream(newFile);
			byte[] buffer = new byte[10 * 1024];
			for (int length; (length = inputStream.read(buffer)) != -1;) {
				outputStream.write(buffer, 0, length);
				outputStream.flush();
			}
			
			insertFile(newFile);

		} catch (FileNotFoundException exception) {
			exception.printStackTrace();
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	/**
	 * Insere o arquivo no S3
	 */
	public PutObjectResult insertFile(File file) {
		AmazonS3 s3Client = buildS3Client();
				
		PutObjectRequest objRequest = new PutObjectRequest(
				ParametersUtil.recupera(PARAM_BUCKET_NAME), file.getName(), file);
		objRequest.setCannedAcl(CannedAccessControlList.PublicRead);		
		PutObjectResult result = s3Client.putObject(objRequest);		
		return result;
	}
	
	public void excluir(Arquivo selectedArquivo) {
		AmazonS3 s3Client = buildS3Client();		
		s3Client.deleteObject(new DeleteObjectRequest(ParametersUtil.recupera(PARAM_BUCKET_NAME), selectedArquivo.getKey()));		
	}

	public List<Arquivo> find(String key) {
		List<Arquivo> arquivos = findAll();
		Iterator<Arquivo> iterator = arquivos.iterator();
		while(iterator.hasNext()){
			Arquivo current = iterator.next();
			if(!current.getKey().contains(key)){
				iterator.remove();
			}
		}		
		
		return arquivos;
	}	 
	
	public List<Arquivo> findAll() {
		List<Arquivo> arquivos = new ArrayList<Arquivo>();
		AmazonS3 s3client = buildS3Client();

		try {			
			final ListObjectsV2Request req = new ListObjectsV2Request()
					.withBucketName(ParametersUtil.recupera(PARAM_BUCKET_NAME)).withMaxKeys(2);
			ListObjectsV2Result result;
			do {
				result = s3client.listObjectsV2(req);

				for (S3ObjectSummary objectSummary : result
						.getObjectSummaries()) {
					arquivos.add(buildNewArquivo(objectSummary));
				}				
				req.setContinuationToken(result.getNextContinuationToken());
			} while (result.isTruncated() == true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return arquivos;
	}	

	public String getUrlFile(Arquivo arquivo) {
		try {			
			@SuppressWarnings({ "deprecation", "resource" })
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(ParametersUtil.recupera(PARAM_ZENCODER_URL));
			// add header
			post.setHeader("Content-Type", "application/json");
			post.setHeader("Zencoder-Api-Key", ParametersUtil.recupera(PARAM_ZENCODER_KEY));					

			JSONObject jsonObject = new JSONObject();
			JSONArray outputs = new JSONArray();
			JSONObject jsonAccessControl = new JSONObject();
			jsonAccessControl.put("permission", READ_PERMISSION);
			jsonAccessControl.put("grantee", ParametersUtil.recupera(PARAM_ZENCODER_GRANTEE));
			outputs.put(jsonAccessControl);
			jsonObject.put("test", "true");
			jsonObject.put("input", ParametersUtil.recupera(PARAM_BUCKET_URL) + arquivo.getKey());
			jsonObject.put("outputs", outputs);

			StringEntity se = new StringEntity(jsonObject.toString());			
			post.setEntity(se);

			HttpResponse response = client.execute(post);

			BufferedReader rd = new BufferedReader(new InputStreamReader(
					response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}			

			JSONObject obj = new JSONObject(result.toString());
			JSONArray outputsResponse = new JSONArray(obj.get("outputs")
					.toString());
			if (outputsResponse.length() > 0) {
				JSONObject resultado = outputsResponse.getJSONObject(0);
				return resultado.getString("url");
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Obtem diretorio temporario do ambiente.
	 * 
	 */
	public String getTempDir() {
		String temp = FacesContext.getCurrentInstance().getExternalContext().getRealPath("");
		String index = "tmp";		
		int i = temp.indexOf(index);
		String dirTemp = temp.substring(0, i)+index;		
		return dirTemp + File.separator;
	}
	
	/**
	 * Cria o client S3 com as credenciais necessarias.
	 *  
	 */
	private AmazonS3Client buildS3Client() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(ParametersUtil.recupera(PARAM_ACCESS_KEY),
				ParametersUtil.recupera(PARAM_SECRET_KEY));
		return new AmazonS3Client(awsCreds);
	}	
	
	/**
	 * Converte o objeto S3 em um objeto Arquivo. 
	 * 
	 */
	private Arquivo buildNewArquivo(S3ObjectSummary objectSummary) {
		Arquivo arquivo = new Arquivo();
		arquivo.setKey(objectSummary.getKey());

		long size = objectSummary.getSize();
		if (size > 0) {
			arquivo.setSize((double) (size / ONE_MB));
		} else {
			arquivo.setSize((double) size);
		}

		return arquivo;
	}	
	
}
