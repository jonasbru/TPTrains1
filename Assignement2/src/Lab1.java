
import TSim.*;
import java.util.concurrent.Semaphore;

public class Lab1 {

    public static int simulationSpeed;

    public static void main(String[] args) {
        new Lab1(args);
    }

    public Lab1(String[] args) {
        if (args.length != 2 && args.length != 3) {
            System.err.println("2 or 3 arguments !!!!!");
            return;
        }

        if (args.length == 3) {
            simulationSpeed = Integer.parseInt(args[2]);
        } else {
            simulationSpeed = 100;
        }

        Train t1 = new Train(1, Integer.parseInt(args[0]));
        Train t2 = new Train(2, Integer.parseInt(args[1]));

        t1.start();
        t2.start();
    }
}

class Train extends Thread {

    public static Semaphore cs1 = new Semaphore(1); //Critical Section 1
    public static Semaphore cs2 = new Semaphore(1); //Critical Section 2
    public static Semaphore cs3 = new Semaphore(1); //Critical Section 3
    public static Semaphore tw1 = new Semaphore(0); //Two Ways section 1
    public static Semaphore tw2 = new Semaphore(1); //Two Ways section 2
    public static Semaphore tw3 = new Semaphore(0); //Two Ways section 3
    int id; //Train id
    int speed;
    TAKEN_CS takenCS = TAKEN_CS.NONE; //None of the trains start in a CS
    TAKEN_TW takenTW;
    STATION station;
    boolean fast = true; //While in a TW Section, allows to know in which of the two ways the train is

    enum TAKEN_CS {

        SC1,
        SC2,
        SC3,
        NONE
    }

    enum TAKEN_TW {

        IN1,
        IN2,
        IN3,
        NONE
    }

    enum STATION {

        UP,
        DOWN,
        NONE
    }

    public Train(int id, int speed) {
        this.id = id;
        this.speed = speed;
    }

    private void init() throws CommandException {
        if (this.id == 1) {
            this.takenTW = TAKEN_TW.IN3;
            this.station = STATION.UP;
        } else if (this.id == 2) {
            this.takenTW = TAKEN_TW.IN1;
            this.station = STATION.DOWN;
        }

        TSimInterface.getInstance().setSpeed(this.id, this.speed);
    }

    @Override
    public void run() {
        TSimInterface tsi = TSimInterface.getInstance();
        try {
            init();
            while (true) {
                SensorEvent se = tsi.getSensor(this.id);
                if (se.getStatus() == SensorEvent.ACTIVE) {
                    manageStations(se, tsi);
                    manageCriticalSections(se, tsi);
                    manageTwoWaysSections(se, tsi);
                }
            }
        } catch (CommandException e) {
            e.printStackTrace(); // or only e.getMessage() for the error
            System.exit(1);
        } catch (InterruptedException e) {
            e.printStackTrace(); // or only e.getMessage() for the error
            System.exit(1);
        }
    }

    private void manageStations(SensorEvent se, TSimInterface tsi) throws CommandException, InterruptedException {
        if (se.getXpos() == 15 && se.getYpos() == 3
                || se.getXpos() == 15 && se.getYpos() == 5) { //Stations section
            if (station == STATION.UP) {
                station = STATION.NONE;
            } else {
                station = STATION.UP;
                tsi.setSpeed(this.id, 0);
                this.sleep(2 + 2 * Lab1.simulationSpeed * Math.abs(this.speed));
                this.speed = this.speed * -1;
                tsi.setSpeed(this.id, this.speed);
            }
        } else if (se.getXpos() == 15 && se.getYpos() == 11
                || se.getXpos() == 15 && se.getYpos() == 13) {
            if (station == STATION.DOWN) {
                station = STATION.NONE;
            } else {
                station = STATION.DOWN;
                tsi.setSpeed(this.id, 0);
                this.sleep(2 + 2 * Lab1.simulationSpeed * Math.abs(this.speed));
                this.speed = this.speed * -1;
                tsi.setSpeed(this.id, this.speed);
            }
        }
    }

    private void manageCriticalSections(SensorEvent se, TSimInterface tsi) throws CommandException, InterruptedException {
        if (se.getXpos() == 9 && se.getYpos() == 5 //SC3
                || se.getXpos() == 11 && se.getYpos() == 7
                || se.getXpos() == 6 && se.getYpos() == 6
                || se.getXpos() == 10 && se.getYpos() == 8) {
            if (takenCS == TAKEN_CS.SC3) {
                cs3.release();
                takenCS = TAKEN_CS.NONE;
            } else {
                testCriticalSection(cs3);
                takenCS = TAKEN_CS.SC3;
            }
        } else if (se.getXpos() == 14 && se.getYpos() == 7) { //SC2
            if (takenCS == TAKEN_CS.SC2) {
                cs2.release();
                takenCS = TAKEN_CS.NONE;
            } else {
                testCriticalSection(cs2);
                takenCS = TAKEN_CS.SC2;
                tsi.setSwitch(17, 7, TSimInterface.SWITCH_RIGHT);
            }
        } else if (se.getXpos() == 15 && se.getYpos() == 8) {
            if (takenCS == TAKEN_CS.SC2) {
                cs2.release();
                takenCS = TAKEN_CS.NONE;
            } else {
                testCriticalSection(cs2);
                takenCS = TAKEN_CS.SC2;
                tsi.setSwitch(17, 7, TSimInterface.SWITCH_LEFT);
            }
        } else if (se.getXpos() == 12 && se.getYpos() == 9) {
            if (takenCS == TAKEN_CS.SC2) {
                cs2.release();
                takenCS = TAKEN_CS.NONE;
            } else {
                testCriticalSection(cs2);
                takenCS = TAKEN_CS.SC2;
                tsi.setSwitch(15, 9, TSimInterface.SWITCH_RIGHT);
            }
        } else if (se.getXpos() == 13 && se.getYpos() == 10) {
            if (takenCS == TAKEN_CS.SC2) {
                cs2.release();
                takenCS = TAKEN_CS.NONE;
            } else {
                testCriticalSection(cs2);
                takenCS = TAKEN_CS.SC2;
                tsi.setSwitch(15, 9, TSimInterface.SWITCH_LEFT);
            }
        } else if (se.getXpos() == 7 && se.getYpos() == 9) { //SC1
            if (takenCS == TAKEN_CS.SC1) {
                cs1.release();
                takenCS = TAKEN_CS.NONE;
            } else {
                testCriticalSection(cs1);
                takenCS = TAKEN_CS.SC1;
                tsi.setSwitch(4, 9, TSimInterface.SWITCH_LEFT);
            }
        } else if (se.getXpos() == 6 && se.getYpos() == 10) {
            if (takenCS == TAKEN_CS.SC1) {
                cs1.release();
                takenCS = TAKEN_CS.NONE;
            } else {
                testCriticalSection(cs1);
                takenCS = TAKEN_CS.SC1;
                tsi.setSwitch(4, 9, TSimInterface.SWITCH_RIGHT);
            }
        } else if (se.getXpos() == 6 && se.getYpos() == 11) {
            if (takenCS == TAKEN_CS.SC1) {
                cs1.release();
                takenCS = TAKEN_CS.NONE;
            } else {
                testCriticalSection(cs1);
                takenCS = TAKEN_CS.SC1;
                tsi.setSwitch(3, 11, TSimInterface.SWITCH_LEFT);
            }
        } else if (se.getXpos() == 4 && se.getYpos() == 13) {
            if (takenCS == TAKEN_CS.SC1) {
                cs1.release();
                takenCS = TAKEN_CS.NONE;
            } else {
                testCriticalSection(cs1);
                takenCS = TAKEN_CS.SC1;
                tsi.setSwitch(3, 11, TSimInterface.SWITCH_RIGHT);
            }
        }
    }

    private void manageTwoWaysSections(SensorEvent se, TSimInterface tsi) throws CommandException, InterruptedException {
        if (se.getXpos() == 19 && se.getYpos() == 8) { //IN3
            if (takenTW == TAKEN_TW.IN3) {
                if (fast) {
                    tw3.release();
                }
                takenTW = TAKEN_TW.NONE;
            } else {
                chooseFreeWay(tw3, 17, 7, TSimInterface.SWITCH_RIGHT);
                takenTW = TAKEN_TW.IN3;
            }
        } else if (se.getXpos() == 18 && se.getYpos() == 9) { //IN2
            if (takenTW == TAKEN_TW.IN2) {
                if (fast) {
                    tw2.release();
                }
                takenTW = TAKEN_TW.NONE;
            } else {
                chooseFreeWay(tw2, 15, 9, TSimInterface.SWITCH_RIGHT);
                takenTW = TAKEN_TW.IN2;
            }
        } else if (se.getXpos() == 1 && se.getYpos() == 9) {
            if (takenTW == TAKEN_TW.IN2) {
                if (fast) {
                    tw2.release();
                }
                takenTW = TAKEN_TW.NONE;
            } else {
                chooseFreeWay(tw2, 4, 9, TSimInterface.SWITCH_LEFT);
                takenTW = TAKEN_TW.IN2;
            }
        } else if (se.getXpos() == 1 && se.getYpos() == 10) { //IN1
            if (takenTW == TAKEN_TW.IN1) {
                if (fast) {
                    tw1.release();
                }
                takenTW = TAKEN_TW.NONE;
            } else {
                chooseFreeWay(tw1, 3, 11, TSimInterface.SWITCH_LEFT);
                takenTW = TAKEN_TW.IN1;
            }
        }
    }

    private void testCriticalSection(Semaphore s) throws CommandException, InterruptedException {
        TSimInterface tsi = TSimInterface.getInstance();
        if (s.tryAcquire() == false) {
            tsi.setSpeed(this.id, 0);
            s.acquire();
            tsi.setSpeed(this.id, this.speed);
        }
    }

    private void chooseFreeWay(Semaphore s, int xin, int yin, int fastestWay) throws CommandException, InterruptedException {
        TSimInterface tsi = TSimInterface.getInstance();
        int otherWay;
        if (fastestWay == TSimInterface.SWITCH_LEFT) {
            otherWay = TSimInterface.SWITCH_RIGHT;
        } else {
            otherWay = TSimInterface.SWITCH_LEFT;
        }

        if (s.tryAcquire() == false) { // Fastest way occuped
            tsi.setSwitch(xin, yin, otherWay);
            fast = false;
        } else { // We take the fastest way
            tsi.setSwitch(xin, yin, fastestWay);
            fast = true;
        }
    }
}
