s.options.numBuffers = 16000;
s.options.memSize = 655360;

s.boot;
().play;


~safesync = {
	try {
		s.sync;
	};
};

s.waitForBoot({

	().play;
	~crazylady = Buffer.read(s,"/home/hindle1/Music/more-crazy-lady.wav");
	~buffers = "/home/hindle1/projects/grains-of-voxels/oldrandtux/rand-tux-15009.wav
/home/hindle1/projects/grains-of-voxels/oldrandtux/rand-tux-23138.wav
/home/hindle1/projects/grains-of-voxels/oldrandtux/rand-tux-28198.wav
/home/hindle1/projects/grains-of-voxels/oldrandtux/rand-tux-6833.wav
/home/hindle1/projects/grains-of-voxels/oldrandtux/rand-tux-4964.wav
/home/hindle1/projects/grains-of-voxels/oldrandtux/rand-tux-952.wav".split($\n).collect({
	|filename| Buffer.read(s,filename)
});
	
	~safesync.();
	
	SynthDef(\g1rainer,
		{
			arg trate,d,b,rate=1.2,amp=1.0,out=0,gamp=1.0;
			var dur;
			dur = (10.rand + 1.0) / (2*trate);
			Out.ar(out,
				gamp * TGrains.ar(2,
					Impulse.ar(trate), // trigger
					b, // buffer
					(rate ** WhiteNoise.kr(3).round(1)), // rate
					1.0.rand + d*BufDur.kr(b), //center
					//d*BufDur.kr(b),
					dur, //duration
					WhiteNoise.kr(0.6),//pan
					0.1*amp, //amp
					2
				);
			);
		}).load;

	~safesync.();
	
	~ab = Bus.audio(s,2);
	// play to bus
	o = { arg mul=100.0,mulin=100.0,amp=0.9; Out.ar(0,amp*mul*Clip.ar(mulin*In.ar(~ab.index),-1.0/mul,1.0/mul)!2) }.scope;
	// short noise burst
	(\out: ~ab.index,\amp: 100.rand, \dur: 0.01,\freq: 4.collect { 240.linrand+20 } ).play;
	//

	~safesync.();

	

	~synths = 20.collect {
		var mysynth = Synth(\g1rainer,[\trate,10,\d,1.0.rand,\b,~buffers.choose,\rate,1.0.rand + 1,\amp,1.0,\out,~ab.index,\gamp,0.0]);
		mysynth.set(\trate,5.0.rand);
		mysynth
	};

	~safesync.();


	~synths.do {|u| 
		u.set(\trate,12.0.rand);
		u.set(\d,1.0.rand);
		u.set(\rate,0.5.linrand + 0.5);
	};

	~rampup1 = {
		|u,dur=10,mini=0.00001,maxi=1.0|
		Routine({
			100.do {|x|
				var amp = (x/100.0).linexp(0.0,1.0,mini,maxi);
				//amp.postln;
				u.set(\gamp,amp);		
				(dur/100.0).wait;
			};
		}).play;
	};
	~rampdown1 = {
		|u,dur=10,mini=0.00001|
		Routine({
			100.do {|x|
				var amp = (x/100.0).linexp(0.0,1.0,1.0,mini);
				u.set(\gamp,amp);
				(dur/100.0).wait;
			};
		}).play;
	};

	//~rampup1.(~synths[1]);
	//~rampdown1.(~synths[1]);
	
	~rampup = {
		|dur=10,min=0.00001,maxi=1.0|
		~synths.do {|u|
			~rampup1.(u,dur,min,maxi)
		}
	};
	// ~rampup.()
	~rampdown = {
		|dur=10,min=0.000001|
		~synths.do {|u|
			~rampdown1.(u,dur,min)
		}
	};
	// ~rampdown.()
	~safesync.();
	
	
	~start = {
		~crazyladysynth = { PlayBuf.ar(1, ~crazylady, BufRateScale.kr(~crazylady), doneAction: 2) }.play;

		Routine({
			~synths.do {|u|
				u.set(\gamp,0.0)
			};
			~synths.do {|u|
				u.postln;
				~rampup1.(u,10,0.00001,0.01);
				5.0.wait;
			};
			"Rampup!".postln;
			~rampup.(10.0,0.01,1.0);
			10.0.wait;
			"Startup done!".postln;
		}).play();
		Routine({ // timing
			70.do {|x|
				[(x/70.0*100),"position"].postln;
				10.wait;
			};
			if(~finishrunning,{
				"Already finishing!".postln;
			},{
				"Running finish!".postln;
				~finish.();
			})
		}).play();
	};

	// ~start.();
	// ~start.stop;
	~finish = {
		~finishrunning = true;		
		Routine({
			~rampdown.(20);
			20.wait;
			(38..64).do {|x|
				MIDIIn.doNoteOnAction(1, 0, 127.linrand, x);
				
			};
			~synths.do {|u|
				u.set(\gamp,0.0)
			};
			~finishrunning = false;
			~crazyladysynth.stop;
		}).play();
	};
	// ~finish.();
	
	/*
		~synths.do {|u| 
		u.set(\gamp,1);
		};
		~synths.do {|u| 
		u.set(\gamp,0.001);
		};

		~synths.do {|u| 
		u.set(\gamp,1);
		};
		
	*/
	
	MIDIIn.connectAll;
	~safesync.();
	MIDIdef.noteOn(\test4, {arg ...args; args.postln}); // match any noteOn
	//MIDIIn.doNoteOnAction(1, 1, 64, 64); // spoof a note on
	//MIDIIn.doNoteOnAction(1, 1, 127.linrand, 64); // spoof a note on
	
	MIDIdef.noteOn(\synths, {arg ...args; args.postln;
		"changing grains".postln;
		~synths.do {|u| 
			u.set(\trate,2.0.rand);
			u.set(\d,1.0.rand);
			u.set(\rate,0.5.linrand + 0.5);
			u.set(\amp,1.0.linrand);
		};
	},nil,1); // match any noteOn
	
	
	~noises = 128.collect { nil };
	~freenoise = {|i| var v = ~noises[i]; ~noises[i] = nil; if(v!=nil,{v.release(0.01);}) };
	MIDIdef.noteOn(\noiseon, {arg ...args;
		var syn, note = args[1];
		["noteon",note].postln;
		args.postln;
		syn = (\out: ~ab.index,\amp: 10.rand, \dur: 10,\freq: 4.collect { note.midicps + (20.0.linrand) } ).play;
		~freenoise.(note);
		~noises[note] = syn;
	},nil,0); // match any noteOn
	MIDIdef.noteOff(\noiseoff, {arg ...args;
		var note = args[1];
		["noteoff",note].postln;
		~noises[note].postln;
		~freenoise.(note);
	},nil,0); // match any noteOn
	//MIDIIn.doNoteOnAction(1, 1, 46, 64); // spoof a note on
	//MIDIIn.doNoteOffAction(1, 1, 46, 64); // spoof a note on
	//~freenoise.(0)

	/*

	*/
	
});

/*
	~start.();

	// change a grain
	MIDIIn.doNoteOnAction(1, 1, 127.linrand, 127.linrand);

		// Randomly trigger a change
		Routine({
		   100.do {
		       100.linrand.wait;
		       "Random grain!".postln;
		       MIDIIn.doNoteOnAction(1, 1, 127.linrand, 127.linrand);
		   };
		}).play;

	(80..100).do {|x|
				MIDIIn.doNoteOnAction(1, 0, x, 127);
	};
	(80..100).do {|x|
				MIDIIn.doNoteOffAction(1, 0, x, 127);
	};


	Routine({
	   var v = 117.rand;
	   ((v)..(v+10)).do {|x|
	      MIDIIn.doNoteOnAction(1, 0, x, x);
	   };
	   0.5.wait;
	   ((v)..(v+10)).do {|x|
	      MIDIIn.doNoteOffAction(1, 0, x, x);
	   };
	}).play;

	~synths.do {|u| 
	   u.set(\trate,12.0.rand);
	   u.set(\d,1.0.rand);
	   u.set(\rate,0.3.linrand + 0.2);
	   u.set(\amp,0.01.linrand);
	};
	{ Out.ar(~ab.index,PlayBuf.ar(1, ~crazylady, BufRateScale.kr(~crazylady), doneAction: 2)) }.play;
	(\out: ~ab.index,\amp: 100.rand, \dur: 0.02,\freq: 4.collect { 620.linrand+20 } ).play;

	// silence!
	(\out: ~ab.index,\amp: 100.rand, \dur: 0.001.linrand,\freq: 4.collect { 1.0.linrand } ).play;


	~finish.();
*/

		~finishrunning = false;		

