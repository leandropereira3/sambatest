/**
 * 
 */
package br.com.sambatest.web.visao;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import lombok.Getter;
import lombok.Setter;

import org.primefaces.model.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;

import br.com.sambatest.core.model.Arquivo;
import br.com.sambatest.core.service.ArquivoService;

/**
 * @author leandro.pereira
 *
 */

@Controller
@RequestScoped
public class VideoController implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6652263107707848041L;

	@Autowired
	private ArquivoService service;	
	
	@Getter
	@Setter
	private Arquivo arquivo;
	
	@Getter
	@Setter
	private List<Arquivo> arquivosList;
	
	@Getter
	@Setter
	private UploadedFile file;
	
	@Getter
	@Setter
	private String keyConsulta;
	
	@Getter
	@Setter
	private Arquivo arquivoSelecionado;	

	/**
	 * Envia o arquivo e atualiza o grid.
	 */
	public void upload(){
		service.uploadFile(file);
		find();
		showMensagemInformacao("Operação realizada com sucesso!");
	}
	
	/**
	 * Consulta todos os registros ou um registro especifico.
	 */
	public void find(){
		if(StringUtils.hasText(keyConsulta)){
			setArquivosList(service.find(keyConsulta));
		}
		else{
			setArquivosList(service.findAll());			
		}
	}
	
	/**
	 * Exclui o arquivo selecionado e atualiza a listagem.
	 */
	public void excluir(Arquivo selectedArquivo){
		service.excluir(selectedArquivo);
		find();
		showMensagemInformacao("Operação realizada com sucesso!");
	}
	
	
	public void abrirArquivo(Arquivo selectedArquivo){
		setArquivoSelecionado(selectedArquivo);
		getArquivoSelecionado().setUrl(service.getUrlFile(arquivoSelecionado));
		System.out.println(arquivoSelecionado.getUrl());		
		
		ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
		waitSeconds(); //FIXME: Atrasa o andamento do processamento. Por algum motivo a url gerada não funciona nos primeiros segundos.   	
		
		try {
			externalContext.redirect(arquivoSelecionado.getUrl());
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	/**
	 * Atrasa o processamento por 20 segundos
	 */
	private void waitSeconds() {
		try {
        	Thread t = new Thread();
    		t.sleep(20000);			
		
		} catch (InterruptedException e) {		
			e.printStackTrace();
		}
		
	}

	private void showMensagemInformacao(String msg){
		FacesContext.getCurrentInstance().addMessage("informacao",
				new FacesMessage(FacesMessage.SEVERITY_INFO, msg,""));
	}

}
