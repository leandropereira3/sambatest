/**
 * 
 */
package sambatest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import br.com.sambatest.core.model.Arquivo;
import br.com.sambatest.core.service.ArquivoService;
import br.com.sambatest.core.service.ArquivoServiceImpl;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;

/**
 * @author leandro.pereira
 *
 */


public class ArquivoServiceTest {

	static final String BUCKET_NAME = "s3sambatest";
	static final String ACCESS_KEY = "AKIAICTH7VKHTNS6O5OQ";
	static final String SECRET_KEY = "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";
	static final String FILE_NAME = "fileTest.txt";
		
	ArquivoService service = new ArquivoServiceImpl();

	@Test
	public void insertFile() throws IOException {
		String keyName = "insertJunitKey";				
		File tempFile = buildFile(keyName);
		
		PutObjectResult result = service.insertFile(tempFile);
		Assert.assertNotNull(result);			
	}	
	
	@Test
	public void removeFile() throws IOException{
		String keyName = "removeJunitKey";				
		File tempFile = buildFile(keyName);
		
		AmazonS3 s3Client = buildS3Client();				
		PutObjectResult result = s3Client.putObject(new PutObjectRequest(BUCKET_NAME, keyName, tempFile));
		Assert.assertNotNull(result);
		
		Arquivo arquivo = new Arquivo();
		arquivo.setKey(keyName);	
		
		service.excluir(arquivo);
		
		S3Object object = null;
		try{
			object = s3Client.getObject(
	                new GetObjectRequest(BUCKET_NAME, arquivo.getKey()));
		}
		catch(AmazonS3Exception e){
			
		}
		Assert.assertNull(object);	
	}

	@Test
	public void findFile() throws IOException{
		String keyName = "findJunitKey";				
		File tempFile = buildFile(keyName);
		
		AmazonS3 s3Client = buildS3Client();				
		PutObjectResult result = s3Client.putObject(new PutObjectRequest(BUCKET_NAME, keyName, tempFile));
		Assert.assertNotNull(result);	
		   
		List<Arquivo> arquivos = service.find(keyName);
		
		Assert.assertFalse(arquivos.isEmpty());		
	}
	
	@Test
	public void conectZencoder(){	
		try{
			String url = "https://app.zencoder.com/api/v2/jobs";
			
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(url);

			// add header		
			post.setHeader("Content-Type", "application/json");
			post.setHeader("Zencoder-Api-Key", "764c9727ee6186b5a53097685af5fa99");	

			JSONObject jsonObject = new JSONObject();
			JSONArray outputs = new JSONArray();			
			
			JSONObject jsonAccessControl = new JSONObject();
			jsonAccessControl.put("permission", "READ");
			jsonAccessControl.put("grantee", "leandro.pereira3@gmail.com");			
			outputs.put(jsonAccessControl);
			
			jsonObject.put("test", "true");
			jsonObject.put("input", "s3://s3sambatest/sample.dv");
			jsonObject.put("outputs", outputs);
			
			StringEntity se = new StringEntity(jsonObject.toString());
			
			post.setEntity(se);

			HttpResponse response = client.execute(post);
			System.out.println("Response Code : " 
		                + response.getStatusLine().getStatusCode());

			BufferedReader rd = new BufferedReader(
			        new InputStreamReader(response.getEntity().getContent()));

			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}			
			
			JSONObject obj = new JSONObject(result.toString());
			JSONArray outputsResponse = new JSONArray(obj.get("outputs").toString());
			if(outputsResponse.length() > 0){
				JSONObject resultado = outputsResponse.getJSONObject(0);
				resultado.get("url");
				
				Assert.assertNotNull(resultado.toString());
			}					
			
			System.out.println(result.toString());
			}
			catch(Exception e){
				e.printStackTrace();
				Assert.fail();
			}
	}
	
	
	
	public AmazonS3Client buildS3Client(){
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(ACCESS_KEY,
				SECRET_KEY);				
		return new AmazonS3Client(awsCreds);		
	}
	
	private File buildFile(String fileName) throws IOException {
		File tempFile = new File(getTempDir()+fileName);		
		tempFile.createNewFile();
		
		return tempFile;
	}
	
	public String getTempDir(){
		return System.getProperty("java.io.tmpdir")+File.separator;
	}
}
