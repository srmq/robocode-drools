/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drools_robocode;

import java.util.Vector;
import org.drools.KnowledgeBase;
import org.drools.KnowledgeBaseFactory;
import org.drools.builder.KnowledgeBuilder;
import org.drools.builder.KnowledgeBuilderFactory;
import org.drools.builder.ResourceType;
import org.drools.io.ResourceFactory;
import org.drools.runtime.StatefulKnowledgeSession;
import org.drools.runtime.rule.FactHandle;
import org.drools.runtime.rule.QueryResultsRow;
import robocode.AdvancedRobot;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.RobotStatus;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;

/**
 *
 * @author ribadas
 */
public class RobotDrools extends AdvancedRobot {

    public static String FICHERO_REGLAS = "drools_robocode/reglas/reglas_robot.drl";
    public static String CONSULTA_ACCIONES = "consulta_acciones";
    
    private KnowledgeBuilder kbuilder;
    private KnowledgeBase kbase;   // Base de conocimeintos
    private StatefulKnowledgeSession ksession;  // Memoria activa
    private Vector<FactHandle> referenciasHechosActuales = new Vector<FactHandle>();

    
    public RobotDrools(){
    }
    
    @Override
    public void run() {
    	DEBUG.habilitarModoDebug(System.getProperty("robot.debug", "true").equals("true"));    	

    	// Crear Base de Conocimiento y cargar reglas
    	crearBaseConocimiento();

        // Hacer que movimiento de tanque, radar y cañon sean independientes
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setAdjustRadarForRobotTurn(true);


        while (true) {
        	DEBUG.mensaje("inicio turno");
            //cargarEventos();  // se hace en los metodos onXXXXXEvent()
            cargarEstadoRobot();
            cargarEstadoBatalla();

            // Lanzar reglas
            DEBUG.mensaje("hechos en memoria activa");
            DEBUG.volcarHechos(ksession);           
            ksession.fireAllRules();
            limpiarHechosIteracionAnterior();

            // Recuperar acciones
            Vector<Accion> acciones = recuperarAcciones();
            DEBUG.mensaje("acciones resultantes");
            DEBUG.volcarAcciones(acciones);

            // Ejecutar Acciones
            ejecutarAcciones(acciones);
        	DEBUG.mensaje("fin turno\n");
            execute();  // Informa a robocode del fin del turno (llamada bloqueante)

        }

    }


    private void crearBaseConocimiento() {
        String ficheroReglas = System.getProperty("robot.reglas", RobotDrools.FICHERO_REGLAS);

        DEBUG.mensaje("crear base de conocimientos");
        kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder();
        
        DEBUG.mensaje("cargar reglas desde "+ficheroReglas);
        kbuilder.add(ResourceFactory.newClassPathResource(ficheroReglas, RobotDrools.class), ResourceType.DRL);
        if (kbuilder.hasErrors()) {
            System.err.println(kbuilder.getErrors().toString());
        }

        kbase = KnowledgeBaseFactory.newKnowledgeBase();
        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages());
        
        DEBUG.mensaje("crear sesion (memoria activa)");
        ksession = kbase.newStatefulKnowledgeSession();
    }



    private void cargarEstadoRobot() {
    	EstadoRobot estadoRobot = new EstadoRobot(this);
        referenciasHechosActuales.add(ksession.insert(estadoRobot));
    }

    private void cargarEstadoBatalla() {
        EstadoBatalla estadoBatalla =
                new EstadoBatalla(getBattleFieldWidth(), getBattleFieldHeight(),
                getNumRounds(), getRoundNum(),
                getTime(),
                getOthers());
        referenciasHechosActuales.add(ksession.insert(estadoBatalla));
    }

    private void limpiarHechosIteracionAnterior() {
        for (FactHandle referenciaHecho : this.referenciasHechosActuales) {
            ksession.retract(referenciaHecho);
        }
        this.referenciasHechosActuales.clear();
    }

    private Vector<Accion> recuperarAcciones() {
        Accion accion;
        Vector<Accion> listaAcciones = new Vector<Accion>();

        for (QueryResultsRow resultado : ksession.getQueryResults(RobotDrools.CONSULTA_ACCIONES)) {
            accion = (Accion) resultado.get("accion");  // Obtener el objeto accion
            accion.setRobot(this);                      // Vincularlo al robot actual
            listaAcciones.add(accion);
            ksession.retract(resultado.getFactHandle("accion")); // Eliminar el hecho de la memoria activa
        }

        return listaAcciones;
    }

    private void ejecutarAcciones(Vector<Accion> acciones) {
        for (Accion accion : acciones) {
            accion.iniciarEjecucion();
        }
    }

    // Insertar en la memoria activa los distintos tipos de eventos recibidos 
    @Override
    public void onBulletHit(BulletHitEvent event) {
          referenciasHechosActuales.add(ksession.insert(event));
    }

    @Override
    public void onBulletHitBullet(BulletHitBulletEvent event) {
        referenciasHechosActuales.add(ksession.insert(event));
    }

    @Override
    public void onBulletMissed(BulletMissedEvent event) {
        referenciasHechosActuales.add(ksession.insert(event));
    }

    @Override
    public void onHitByBullet(HitByBulletEvent event) {
        referenciasHechosActuales.add(ksession.insert(event));
    }

    @Override
    public void onHitRobot(HitRobotEvent event) {
        referenciasHechosActuales.add(ksession.insert(event));
    }

    @Override
    public void onHitWall(HitWallEvent event) {
        referenciasHechosActuales.add(ksession.insert(event));
    }

    @Override
    public void onRobotDeath(RobotDeathEvent event) {
        referenciasHechosActuales.add(ksession.insert(event));
    }

    @Override
    public void onScannedRobot(ScannedRobotEvent event) {
        referenciasHechosActuales.add(ksession.insert(event));
    }


}
