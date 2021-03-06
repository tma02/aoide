package tech.aoide.vm.jsinterpreter;

import jdk.nashorn.api.scripting.JSObject;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import tech.aoide.audio.AudioNode;
import tech.aoide.audio.AudioTrack;
import tech.aoide.music.Chord;
import tech.aoide.music.Key;
import tech.aoide.music.Wave;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;

public class JSVM {

    private ScriptEngineManager engineManager;
    private ScriptEngine engine;

    public JSVM() {
    }

    public ArrayList<AudioTrack> eval(String source) throws ScriptException {
        this.engineManager = new ScriptEngineManager();
        this.engine = engineManager.getEngineByName("nashorn");
        ArrayList<AudioTrack> chords = new ArrayList<>();
        Chord chord = Chord.I;
        try {
            Key key = Key.values()[source.length() % Key.values().length];
            String sourcePrefix = "var console = { log: function() { return; } };";
            engine.put("source", sourcePrefix + source);
            engine.eval(new FileReader("acorn_interpreter.js"));
            engine.eval("var interp = new Interpreter(source);");

            while (engine.eval("interp.step();").equals(true)) {
                JSObject stateStack = (JSObject) ((JSObject)engine.get("interp")).getMember("stateStack");
                Object[] stateStackValues = stateStack.values().toArray();
                JSObject node = ((ScriptObjectMirror) ((JSObject) stateStackValues[stateStackValues.length - 1]).getMember("node"));
                Number start = (Number) node.getMember("start");
                Number end = (Number) node.getMember("end");
                if (start.intValue() < 37 || end.intValue() < 37) {
                    continue;
                }

                String code = source.substring(start.intValue() - sourcePrefix.length(), end.intValue() - sourcePrefix.length());
                AudioTrack track = new AudioTrack();
                track.setCodeStart(start.intValue() - sourcePrefix.length());
                track.setCodeEnd(end.intValue() - sourcePrefix.length());
                int duration = Math.max(2, Math.min(6, code.split("\n").length));

                for (int i = 0; i < Math.min(6, code.length()); i++) {
                    int offset = 0;
                    switch (i) {
                        case 0:
                            offset = 0;
                            break;
                        case 1:
                            offset = 2;
                            break;
                        case 2:
                            offset = 4;
                            break;
                        case 3:
                            offset = 7;
                            break;
                        case 4:
                            offset = 9;
                            break;
                        case 5:
                            offset = 11;
                            break;
                    }
                    Wave wave = Wave.SINE;
                    if (code.contains("(") || code.contains(")")) {
                        wave = Wave.SINE;
                    }
                    else if (code.contains(">") || code.contains("<")) {
                        wave = Wave.SAWTOOTH;
                    }
                    else if (code.contains("[") || code.contains("]")) {
                        wave = Wave.SQUARE;
                    }
                    else if (code.contains(".")) {
                        wave = Wave.TRIANGLE;
                    }
                    track.addNode(new AudioNode(key.getNote((chord.ordinal() + offset) % 7) + (Math.max(2, code.getBytes()[0] % 6) + (chord.ordinal() + offset) / 7), wave.name().toLowerCase(), duration));
                }

                chord = Chord.getProgressions(chord)[code.length() % Chord.getProgressions(chord).length];
                chords.add(track);
            }
            return chords;
        } catch (ScriptException e) {
            throw e;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

}
