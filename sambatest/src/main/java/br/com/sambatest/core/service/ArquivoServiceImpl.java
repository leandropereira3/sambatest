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
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.primefaces.model.UploadedFile;
import org.springframework.stereotype.Service;

import br.com.sambatest.core.model.Arquivo;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AccessControlList;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * @author leandro.pereira
 *
 */

@Service
public class ArquivoServiceImpl implements ArquivoService {

	static final String BUCKET_NAME = "s3sambatest";
	static final String BUCKET_NAME_EXT = "s3://s3sambatest/";	
	static final String ACCESS_KEY = "AKIAICTH7VKHTNS6O5OQ";
	static final String SECRET_KEY = "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";
	static final String FILE_NAME = "fileTest.txt";
	static final String ZENCONDER_URL = "https://app.zencoder.com/api/v2/jobs";
	static final String ZENCONDER_KEY = "764c9727ee6186b5a53097685af5fa99";
	static final String READ_PERMISSION = "READ";
	static final String GRANTEE = "leandro.pereira3@gmail.com";

	public void uploadFile(UploadedFile file) {
		
		File newFile = new File(getTempDir() + file.getFileName());

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

	public PutObjectResult insertFile(File file) {
		AmazonS3 s3Client = buildS3Client();
				
		PutObjectRequest objRequest = new PutObjectRequest(
				BUCKET_NAME, file.getName(), file);
		objRequest.setCannedAcl(CannedAccessControlList.PublicRead);		
		PutObjectResult result = s3Client.putObject(objRequest);		
		return result;
	}

	public List<Arquivo> find(String key) {
		List<Arquivo> arquivos = new ArrayList<Arquivo>();
		AmazonS3 s3Client = buildS3Client();
		S3Object object = s3Client.getObject(
                new GetObjectRequest(BUCKET_NAME, key));
		
		try {
			displayTextInputStream(object.getObjectContent());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		return arquivos;
	}
	
	 private static void displayTextInputStream(InputStream input)
			    throws IOException {
			    	// Read one text line at a time and display.
			        BufferedReader reader = new BufferedReader(new 
			        		InputStreamReader(input));
			        while (true) {
			            String line = reader.readLine();
			            if (line == null) break;

			            System.out.println("    " + line);
			        }
			        System.out.println();
			    }
	
	public List<Arquivo> findAll() {
		List<Arquivo> arquivos = new ArrayList<Arquivo>();
		AmazonS3 s3client = buildS3Client();

		try {
			System.out.println("Listing objects");
			final ListObjectsV2Request req = new ListObjectsV2Request()
					.withBucketName(BUCKET_NAME).withMaxKeys(2);
			ListObjectsV2Result result;
			do {
				result = s3client.listObjectsV2(req);

				for (S3ObjectSummary objectSummary : result
						.getObjectSummaries()) {
					arquivos.add(buildNewArquivo(objectSummary));
				}
				System.out.println("Next Continuation Token : "
						+ result.getNextContinuationToken());
				req.setContinuationToken(result.getNextContinuationToken());
			} while (result.isTruncated() == true);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return arquivos;
	}

	private Arquivo buildNewArquivo(S3ObjectSummary objectSummary) {
		Arquivo arquivo = new Arquivo();
		arquivo.setKey(objectSummary.getKey());

		long size = objectSummary.getSize();
		if (size > 0) {
			arquivo.setSize((double) (size / 1000000));
		} else {
			arquivo.setSize((double) size);
		}

		return arquivo;
	}

	private AmazonS3Client buildS3Client() {
		BasicAWSCredentials awsCreds = new BasicAWSCredentials(ACCESS_KEY,
				SECRET_KEY);
		return new AmazonS3Client(awsCreds);
	}
	
	

	public String getUrlFile(Arquivo arquivo) {
		try {
			
			HttpClient client = new DefaultHttpClient();
			HttpPost post = new HttpPost(ZENCONDER_URL);
			// add header
			post.setHeader("Content-Type", "application/json");
			post.setHeader("Zencoder-Api-Key",ZENCONDER_KEY);					

			JSONObject jsonObject = new JSONObject();
			JSONArray outputs = new JSONArray();			

			JSONObject jsonAccessControl = new JSONObject();
			jsonAccessControl.put("permission", READ_PERMISSION);
			jsonAccessControl.put("grantee", GRANTEE);
			outputs.put(jsonAccessControl);
			jsonObject.put("test", "true");
			jsonObject.put("input", BUCKET_NAME_EXT + arquivo.getKey());
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

			System.out.println(result.toString());

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

	public String getTempDir() {
		return System.getProperty("java.io.tmpdir") + File.separator;
	}

	public void excluir(Arquivo selectedArquivo) {
		AmazonS3 s3Client = buildS3Client();			
		
		System.out.println("Removendo arquivo..." + selectedArquivo.getKey());
		s3Client.deleteObject(new DeleteObjectRequest(BUCKET_NAME, selectedArquivo.getKey()));		
		System.out.println("Sucesso!!");
		
	}
}
