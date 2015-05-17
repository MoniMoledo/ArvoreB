/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dojoarvoreb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Carlos Daniel Ogg, Fernando da Rós, João Manoel, Jonatha Nunes,
 * Monique Moledo
 */

public class ArvoreB {    

    /**
     * Executa busca em Arquivos utilizando Arvore B
     * Assumir que ponteiro para próximo nó é igual a -1 quando não houver próximo nó
     * @param codCli: chave do cliente que está sendo buscado
     * @param nomeArquivoMetadados nome do arquivo que contém os metadadso
     * @param nomeArquivoDados nome do arquivo que contém a árvore B
     * @return uma instancia de ResultBusca, preenchida da seguinte forma:
     * Caso a chave codCli seja encontrada:
    encontrou = true
    pont aponta para a página (nó) que contém a chave
    pos aponta para a posição em que a chave se encontra dentro da página (nó)

    Caso a chave codCli não seja encontrada:
    encontrou = false
    pont aponta para a última página (nó) examinada
    pos informa a posição, nessa página, onde a chave deveria estar inserida
     */
    public ResultBusca busca(int codCli, String nomeArquivoMetadados, String nomeArquivoDados) throws Exception {
        RandomAccessFile ArqMeta = new RandomAccessFile(new File(nomeArquivoMetadados), "rw");
        Metadados meta = Metadados.le(ArqMeta);
        RandomAccessFile ArqNos = new RandomAccessFile(new File(nomeArquivoDados), "rw");

        int p = meta.pontRaiz;
        int pos = 0;
        
        ArqNos.seek(p);
        No no = No.le(ArqNos);

        ResultBusca result = new ResultBusca(Integer.MAX_VALUE, Integer.MAX_VALUE, false);
        
        boolean controle = true; 

        while (controle) {

            Cliente monique = no.clientes.get(pos);

            while (pos < no.clientes.size() - 1 && monique.codCliente < codCli) {
                pos++;
                monique = no.clientes.get(pos);
            }
            if (monique.codCliente == codCli) {
                result.encontrou = true;
                controle = false;
            } else {
                int temp;
                if (pos == no.clientes.size() - 1 && monique.codCliente < codCli) {
                    pos++;
                }
                temp = no.p.get(pos);

                if (temp < 0) {
                    controle = false;

                } else {
                    p = temp;
                    ArqNos.seek(p);
                    pos = 0;
                    no = No.le(ArqNos);
                }
            }
        }
        ArqNos.close();
        ArqMeta.close();
        result.pos = pos;
        result.pont = p;
        return result;
    }

    /**
     * Executa inserção em Arquivos Indexados por Arvore B
     * @param codCli: código do cliente a ser inserido
     * @param nomeCli: nome do Cliente a ser inserido
     * @param nomeArquivoMetadados nome do arquivo que contém os metadados
     * @param nomeArquivoDados nome do arquivo de dados que contém a arvore B)
     * @return endereço da folha onde o cliente foi inserido, -1 se não conseguiu inserir
     */
    
    
    private void particiona(String nomeArquivoMetadados, No no, int pontFilhoMenor, RandomAccessFile ArqNos) throws Exception{
                RandomAccessFile ArqMeta = new RandomAccessFile(new File(nomeArquivoMetadados), "rw");
                Metadados meta = Metadados.le(ArqMeta);
                
                ArqNos.seek(meta.pontProxNoLivre);
                No novoNo = new No();
                int pontFilhoMaior = meta.pontProxNoLivre;
                meta.pontProxNoLivre = No.TAMANHO*2;
                
                for (int i = No.d+1; i < no.clientes.size(); i++) {
                    novoNo.clientes.add(no.clientes.get(i));
                    novoNo.p.add(-1);                   
                }
                novoNo.p.add(-1);
                novoNo.m = novoNo.clientes.size();

                for (int i = 0; i < No.d; i++) {
                    no.clientes.remove(No.d+1);
                    no.p.remove(No.d+1);
                }
                no.m = no.clientes.size();
                No pai = null;
                if(no.pontPai<0){ //então o nó é raiz
                    
                   int novoPontRaiz = meta.pontProxNoLivre;
                   meta.pontProxNoLivre = No.TAMANHO*3;
                   meta.pontRaiz = novoPontRaiz;
                   
                   ArqMeta.seek(0);
                   meta.salva(ArqMeta);
                   ArqMeta.close();
                   
                   
                   pai = new No();
                   no.pontPai=novoPontRaiz;
                   novoNo.pontPai = no.pontPai;
                   pai.p.add(pontFilhoMenor);
                }
                
                else {
                    ArqNos.seek(no.pontPai);
                    pai = No.le(ArqNos);
                }
                
                int posCerta = 0; //achar posição ordenada para o cliente q vai subir
                for (int i = 0; i < pai.m; i++) {
                  if(no.clientes.get(No.d).codCliente > pai.clientes.get(i).codCliente){
                    posCerta++;
                  }
            
                }
                pai.clientes.add(posCerta, no.clientes.get(No.d)); 
                no.clientes.remove(No.d);
                no.p.remove(No.d);
                no.m = no.clientes.size();
                novoNo.pontPai = no.pontPai;
                pai.p.add(posCerta+1, pontFilhoMaior);
                pai.m = pai.clientes.size();
                no.m=no.clientes.size();
                ArqNos.seek(pontFilhoMaior);
                novoNo.salva(ArqNos);
                ArqNos.seek(pontFilhoMenor);
                no.salva(ArqNos);
                pontFilhoMenor = no.pontPai;
                ArqNos.seek(pontFilhoMenor);
                pai.salva(ArqNos);
                
                if(pai.clientes.size()> 2*No.d){//verificar se a página/nó ficou cheio
                    particiona(nomeArquivoMetadados, pai, pontFilhoMenor, ArqNos);
            }
                
    };
    
    
    public int insere(int codCli, String nomeCli, String nomeArquivoMetadados, String nomeArquivoDados) throws Exception {
        //TODO: Inserir aqui o código do algoritmo de inserção
        
        ResultBusca busca = busca(codCli, nomeArquivoMetadados, nomeArquivoDados);
        if(busca.encontrou){
            return -1; //retornar -1 pq nao inseriu, ou retornar a pos que o cliente tá?
        }
        else{
            Cliente monique = new Cliente(codCli, nomeCli);
            RandomAccessFile ArqNos = new RandomAccessFile(new File(nomeArquivoDados), "rw");
            
            ArqNos.seek(busca.pont);
            int pontFilhoMenor = busca.pont;
            No no = No.le(ArqNos);
            no.clientes.add(busca.pos, monique);//inseri o cliente na posição aonde a busca assinalou.  
            no.m = no.m +1;//??
            no.p.add(-1);
            
            
            if(no.clientes.size()> 2*No.d){//verificar se a página/nó ficou cheio
                particiona(nomeArquivoMetadados, no, pontFilhoMenor, ArqNos);
            }
            ArqNos.seek(busca.pont);
            no.salva(ArqNos);
            ArqNos.close();
            return busca.pont;
            
        }
       
    }

    /**
     * Executa exclusão em Arquivos Indexados por Arvores B
     * @param codCli: chave do cliente a ser excluído
     * @param nomeArquivoMetadados nome do arquivo que contém os metadados
     * @param nomeArquivoDados nome do arquivo de dados que contém a arvore B)
     * @return endereço do nó de onde o cliente foi excluído, -1 se cliente não existe
     */
    public int exclui(int CodCli, String nomeArquivoMetadados, String nomeArquivoDados) {
        //TODO: Inserir aqui o código do algoritmo de remoção
        return Integer.MAX_VALUE;
    }
}
