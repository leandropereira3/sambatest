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
import br.com.sambatest.core.util.ParametersUtil;

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
	
	static final String PARAM_BUCKET_NAME = "bucket"; 
	static final String PARAM_BUCKET_URL = "urlbucket";
	static final String PARAM_ACCESS_KEY = "accesskey";
	static final String PARAM_SECRET_KEY = "secretkey";
	static final String PARAM_ZENCODER_URL = "zencoderurl";
	static final String PARAM_ZENCODER_KEY = "zencoderkey";
	static final String PARAM_ZENCODER_GRANTEE = "zencodergrantee";	
	static final String READ_PERMISSION = "READ"; 
	static final String ZENCODER_TEST = "zencodertestjob";
	static final String ERROR_CODE = "NoSuchKey";
	
	ArquivoService service = new ArquivoServiceImpl();

	@Test
	public void insertFile() throws IOException {
		String keyName = "insertJunitKey";
		File tempFile = buildFile(keyName);

		PutObjectResult result = service.insertFile(tempFile);
		Assert.assertNotNull(result);
	}

	@Test
	public void removeFile() throws IOException {
		String keyName = "removeJunitKey";
		File tempFile = buildFile(keyName);

		AmazonS3 s3Client = buildS3Client();
		PutObjectResult result = s3Client.putObject(new PutObjectRequest(
				ParametersUtil.recupera(PARAM_BUCKET_NAME), keyName, tempFile));
		Assert.assertNotNull(result);

		Arquivo arquivo = new Arquivo();
		arquivo.setKey(keyName);

		service.excluir(arquivo);

		S3Object object = null;
		try {
			object = s3Client.getObject(new GetObjectRequest(ParametersUtil.recupera(PARAM_BUCKET_NAME),
					arquivo.getKey()));
			Assert.fail();
			
		} catch (AmazonS3Exception e) {
			Assert.assertEquals(ERROR_CODE, e.getErrorCode());
		}		
		
		Assert.assertNull(object);			
	}

	@Test
	public void findFile() throws IOException {
		String keyName = "findJunitKey";
		File tempFile = buildFile(keyName);

		AmazonS3 s3Client = buildS3Client();
		PutObjectResult result = s3Client.putObject(new PutObjectRequest(
				ParametersUtil.recupera(PARAM_BUCKET_NAME), keyName, tempFile));
		Assert.assertNotNull(result);

		List<Arquivo> arquivos = service.find(keyName);

		Assert.assertFalse(arquivos.isEmpty());
	}

	@Test
	public void conectZencoder() {
		try {
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(ParametersUtil.recupera(PARAM_ZENCODER_URL));
			;
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
			jsonObject.put("input", ParametersUtil.recupera(ZENCODER_TEST));
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
				resultado.get("url");

				Assert.assertNotNull(resultado.toString());
			}
			else{
				Assert.fail();
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail();
		}
	}

	public AmazonS3Client buildS3Client() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(ParametersUtil.recupera(PARAM_ACCESS_KEY),
				ParametersUtil.recupera(PARAM_SECRET_KEY));
		return new AmazonS3Client(awsCreds);
	}

	private File buildFile(String fileName) throws IOException {
		File tempFile = new File(getTempDir() + fileName);
		tempFile.createNewFile();

		return tempFile;
	}

	public String getTempDir() {
		return System.getProperty("java.io.tmpdir") + File.separator;
	}
}
