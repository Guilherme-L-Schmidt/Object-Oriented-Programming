package Controler;

import Auxiliar.Consts;
import Auxiliar.Desenho;
import Modelo.Objeto;
import Mapas.Mapa;
import Mapas.MapasNiveis;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ControleDeJogo implements MouseListener, KeyListener {
    private Tela tela;
    private Mapa mapa;
    private Ruler ruler;
    private boolean EnterPress;
    private int numNivelAtual;
    private Deque<ArrayList<Objeto>> movimentosSalvos;
    
    public ControleDeJogo() {
        Desenho.setControle(this);
        this.numNivelAtual = 0;
        this.mapa = new Mapa(MapasNiveis.listaMapas[this.numNivelAtual]);
        this.ruler = new Ruler(this.mapa);
        this.EnterPress = false;
        this.movimentosSalvos = new ArrayDeque<ArrayList<Objeto>>(); 
        
        this.updateAllObjVar();
        
        tela = new Tela(this);
        tela.setVisible(true);
        tela.createBufferStrategy(2);
        tela.go();
    }
    
    public int[] getOffset() {
        if(this.EnterPress) {
            int[] standard_offset = {0, 0, 0};
            return standard_offset;
        }
        return MapasNiveis.offset_bordas[numNivelAtual];
    }
    
    public void updateAllObjVar() {
        this.UpdateObjetoVariavel("Walls/wall_", 10);
        this.UpdateObjetoVariavel("Lava/lava_", 11);
        this.UpdateObjetoVariavel("Grass/grass_", 12);
        this.UpdateObjetoVariavel("Water/water_", 13);
        this.UpdateObjetoVariavel("Brick/brick_", 82);
    }
    
    public void UpdateObjetoVariavel(String name, int code) {
        int[][] matrizObjVars = new int[Consts.RES_VER][Consts.RES_HOR];
        ArrayList<Objeto> objVars = new ArrayList<>();
        ArrayList<Objeto> fase;
        
        if(code < 80)
            fase = mapa.getFaseAtual();
        else
            fase = mapa.getBackgroundAtual();
        for(int i = 0; i < fase.size(); i++) {
            Objeto obj = fase.get(i);
            if(obj.getCode() == code) {
                objVars.add(obj);
                int x = obj.getPosicao().getColuna();
                int y = obj.getPosicao().getLinha();
                
                if(x > 0)
                    matrizObjVars[y][x-1] += 1;
                if(y+1 < Consts.RES_VER)
                    matrizObjVars[y+1][x] += 2;
                if(x+1 < Consts.RES_HOR)
                    matrizObjVars[y][x+1] += 4;
                if(y > 0)
                    matrizObjVars[y-1][x] += 8;                
            }
        }
        
        for(int i = 0; i < objVars.size(); i++) {
            Objeto objVar = objVars.get(i);
            int x = objVar.getPosicao().getColuna();
            int y = objVar.getPosicao().getLinha();
            
            objVar.setName(name + matrizObjVars[y][x]);
        }
    }

    public void UpdateRules() {
        this.ruler.AnalyseRules(this.mapa);
    }
    
    public ArrayList<Objeto> getFaseAtual() {
        return this.mapa.getFaseAtual();
    }
    // Remover e transferir para Mapa
    public void addObject(Objeto umObj) {
        mapa.getFaseAtual().add(umObj);
    }

    public void removeObject(Objeto umObj) {
        mapa.getFaseAtual().remove(umObj);
    }
    
    public void desenhaTudo(ArrayList<Objeto> e) {
        ArrayList<Objeto> background = this.mapa.getBackgroundAtual();
        ArrayList<Objeto> transponiveis = new ArrayList<Objeto>();
        ArrayList<Objeto> outros = new ArrayList<Objeto>();
        ArrayList<Objeto> yous = new ArrayList<Objeto>();
        // loop de separacao
        for(int i = 0; i < e.size(); i++) {
            Objeto obj = e.get(i);
            if(!obj.getStop())
                transponiveis.add(obj);
            else if(obj.getYou())
                yous.add(obj);
            else
                outros.add(obj);
        }
        
        // loops para desenho
        for(int i = 0; i < background.size(); i++)
            background.get(i).autoDesenho();
        for(int i = 0; i < transponiveis.size(); i++)
            transponiveis.get(i).autoDesenho();
        for(int i = 0; i < outros.size(); i++)
            outros.get(i).autoDesenho();
        for(int i = 0; i < yous.size(); i++)
            yous.get(i).autoDesenho();
    }
    
    /*Retorna true se a posicao do objeto eh valida em relacao aos demais no array*/
    public boolean ehPosicaoValida(Objeto obj) {
        Objeto objAnalisado;
        for(int i = 0; i < this.mapa.getFaseAtual().size(); i++) {
            objAnalisado = this.mapa.getFaseAtual().get(i);
            if(objAnalisado != obj && !EnterPress){
                if(objAnalisado.getPosicao().igual(obj.getPosicao())) {
                    if(!analisaColisao(obj, objAnalisado, i))
                        return false;
                }
            }
        }
        return true;
    }
    
    public boolean analisaColisao(Objeto obj1, Objeto obj2, int i) {
        // Check shut and open
        if((obj2.getShut() && obj1.getOpen()) || (obj2.getShut() && obj1.getOpen())) {
            this.mapa.getFaseAtual().remove(i);
            this.mapa.getFaseAtual().remove(obj1);
            this.updateAllObjVar();
        }
        // Check stop
        else if(obj2.getStop()) {
                // Then check push
                if(obj2.getPush()) {
                    switch(obj1.getPosicao().getDirecao()){
                        case 1:
                            return obj2.moveUp();
                        case 2:
                            return obj2.moveRight();
                        case 3:
                            return obj2.moveDown();
                        case 4:
                            return obj2.moveLeft();
                        default:
                            return true;
                    }
                }
            
            return false;
        }
        // Checks hot and melt
        else if((obj2.getMelt() && obj1.getHot())) {
            this.mapa.getFaseAtual().remove(i);
            this.updateAllObjVar();
        }// Checks hot and melt
        else if((obj1.getMelt() && obj2.getHot())) {
            this.mapa.getFaseAtual().remove(obj1);
            this.updateAllObjVar();
        }
        // Check sink
        else if(obj2.getSink() || obj1.getSink()) {
            this.mapa.getFaseAtual().remove(i);
            this.mapa.getFaseAtual().remove(obj1);
            this.updateAllObjVar();
        }
        // Check defeat
        else if(obj2.getYou() && obj1.getDefeat()) {
            this.mapa.getFaseAtual().remove(i);
            this.updateAllObjVar();
        }
        else if (obj2.getDefeat() && obj1.getYou()) {
            this.mapa.getFaseAtual().remove(obj1);
            this.updateAllObjVar();
        }
        // Check win
        else if(obj1.getYou() && obj2.getWin()) {
            Vitoria();
            return false;
        }
        return true;
    }
    
    public void Vitoria() {
        this.mapa.getFaseAtual().clear();
        this.movimentosSalvos.clear();
        this.EnterPress = true;
        this.mapa = new Mapa(MapasNiveis.congratulations);
    }
    
    public void loadFase() {
        this.mapa = new Mapa(MapasNiveis.listaMapas[this.numNivelAtual]);
        this.mapa.setNumNivelAtual(this.numNivelAtual);
        this.ruler = new Ruler(this.mapa);
        this.movimentosSalvos.clear();
        this.updateAllObjVar();
    }
    
    public void updateMapa(Objeto obj) {
        if(obj.getCode() > 20) {
            this.mapa.updateRuleMap(obj);
        }
    }
    
    public void writeToFile(ObjectOutput out) throws IOException{
        out.writeObject(this.mapa);
        out.close();
    }
    
    public void readFile(String fileName) throws FileNotFoundException, IOException, ClassNotFoundException {
        Mapa mapaSalvo = null;
        
        FileInputStream fileIn = new FileInputStream(fileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        
        mapaSalvo = (Mapa) in.readObject();
        
        System.out.println("Jogo carregado com sucesso");
        
        in.close();
        fileIn.close();
        
        this.numNivelAtual = mapaSalvo.numNivelAtual;
        System.out.println("Fase Carregada: " + this.numNivelAtual);
        this.mapa = mapaSalvo;
    }
    public void saveMove(){
        ArrayList<Objeto> mapaAtual = new ArrayList<Objeto>();
        ArrayList<Objeto> fase = this.mapa.getFaseAtual();
        for(int i = 0; i< fase.size(); i++){
            Objeto o;
            try {
                o = fase.get(i).clone();
                o.settPosicao(o.getPosicao().clone());
                mapaAtual.add(o);
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(ControleDeJogo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        this.movimentosSalvos.push(mapaAtual);
    }
    
    public void keyPressed(KeyEvent e) {
        // Reload command
        if (e.getKeyCode() == KeyEvent.VK_R) {
            this.loadFase();
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            System.exit(0);
        }
        if(e.getKeyCode() == KeyEvent.VK_ENTER && this.EnterPress){
            this.EnterPress = false;
            this.numNivelAtual++;
            this.loadFase();
        }
        if(e.getKeyCode() == KeyEvent.VK_Z){
            if(!this.movimentosSalvos.isEmpty()) {
                ArrayList<Objeto> mapaSalvo = this.movimentosSalvos.pop();
                this.mapa.setFaseAtual(mapaSalvo);
                this.mapa.startRuleMap();
                this.updateAllObjVar();
                return;
            }
        }
        if(e.getKeyCode() == KeyEvent.VK_S){
            try(FileOutputStream f = new FileOutputStream("jogoSalvo.txt");
                    ObjectOutputStream s = new ObjectOutputStream(f)){
                this.writeToFile(s);
                System.out.println("Successfuly saved game!");
            }
            catch (FileNotFoundException ex) {
                Logger.getLogger(ControleDeJogo.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(ControleDeJogo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(e.getKeyCode() == KeyEvent.VK_L){
            try {
                this.readFile("jogoSalvo.txt");
            } catch (IOException ex) {
                Logger.getLogger(ControleDeJogo.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ControleDeJogo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        // Deals with movement
        if(e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN || e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_RIGHT) {
            this.saveMove();
            
            ArrayList<Objeto> ordem = new ArrayList<Objeto>();
            for(int i = 0; i < mapa.getFaseAtual().size(); i++) {
                Objeto p = mapa.getFaseAtual().get(i);
                int posl = p.getPosicao().getLinha();
                int posc = p.getPosicao().getColuna();
                int j;

                // System to move in order and prevent colisions
                if(p.getYou()) {
                    if (e.getKeyCode() == KeyEvent.VK_UP) {
                        for(j = 0; j < ordem.size(); j++) {
                            if(posl < ordem.get(j).getPosicao().getLinha())
                                break;
                        }
                        ordem.add(j, p);
                    } else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        for(j = 0; j < ordem.size(); j++) {
                            if(posl > ordem.get(j).getPosicao().getLinha())
                                break;
                        }
                        ordem.add(j, p);
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                        for(j = 0; j < ordem.size(); j++) {
                            if(posc < ordem.get(j).getPosicao().getColuna())
                                break;
                        }
                        ordem.add(j, p);
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                        for(j = 0; j < ordem.size(); j++) {
                            if(posc > ordem.get(j).getPosicao().getColuna())
                                break;
                        }
                        ordem.add(j, p);
                    }
                }
            }

            // moves in the determined order
            for(int i = 0; i < ordem.size(); i++) {
                Objeto p = ordem.get(i);
                if (e.getKeyCode() == KeyEvent.VK_UP)
                    p.moveUp();
                else if (e.getKeyCode() == KeyEvent.VK_DOWN)
                    p.moveDown();
                else if (e.getKeyCode() == KeyEvent.VK_LEFT)
                    p.moveLeft();
                else if (e.getKeyCode() == KeyEvent.VK_RIGHT) 
                    p.moveRight();
            }
            
            
            
        }

        //repaint(); /*invoca o paint imediatamente, sem aguardar o refresh*/
    }
    
 
    
    public void mousePressed(MouseEvent e) {}
    
    public void mouseMoved(MouseEvent e) {}

    public void mouseClicked(MouseEvent e) {}

    public void mouseReleased(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mouseDragged(MouseEvent e) {}

    public void keyTyped(KeyEvent e) {}

    public void keyReleased(KeyEvent e) {}
}
