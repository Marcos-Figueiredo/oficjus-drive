"use client";

import { useState, useCallback } from "react";
import { motion, AnimatePresence } from "framer-motion";
import {
  Check,
  SkipForward,
  List,
  MapPin,
  Navigation2,
  X,
} from "lucide-react";

type BubbleState = "idle" | "confirm" | "skip" | "list";

interface Point {
  id: number;
  label: string;
  status: "pending" | "done" | "skipped";
  x: number;
  y: number;
}

const INITIAL_POINTS: Point[] = [
  { id: 1, label: "Rua das Flores, 120", status: "pending", x: 25, y: 30 },
  { id: 2, label: "Av. Brasil, 450", status: "pending", x: 55, y: 45 },
  { id: 3, label: "Trav. Esperança, 88", status: "pending", x: 40, y: 70 },
  { id: 4, label: "Rua Minas, 302", status: "pending", x: 70, y: 25 },
];

export default function FloatingBubbleDemo() {
  const [points, setPoints] = useState<Point[]>(INITIAL_POINTS);
  const [currentIdx, setCurrentIdx] = useState(0);
  const [bubbleState, setBubbleState] = useState<BubbleState>("idle");
  const [showList, setShowList] = useState(false);
  const [distance, setDistance] = useState(42);

  const currentPoint = points[currentIdx] ?? points[0];

  const advanceToNext = useCallback((updated: Point[], fromIdx: number) => {
    const next = updated.findIndex(
      (p, i) => i > fromIdx && p.status === "pending"
    );
    if (next !== -1) {
      setCurrentIdx(next);
      setDistance(Math.floor(Math.random() * 80) + 15);
    }
    setBubbleState("idle");
  }, []);

  const handleConfirm = useCallback(() => {
    setBubbleState("confirm");
    const idx = currentIdx;
    setPoints((prev) => {
      const updated = prev.map((p, i) =>
        i === idx ? { ...p, status: "done" as const } : p
      );
      setTimeout(() => advanceToNext(updated, idx), 700);
      return updated;
    });
  }, [currentIdx, advanceToNext]);

  const handleSkip = useCallback(() => {
    setBubbleState("skip");
    const idx = currentIdx;
    setPoints((prev) => {
      const updated = prev.map((p, i) =>
        i === idx ? { ...p, status: "skipped" as const } : p
      );
      setTimeout(() => advanceToNext(updated, idx), 700);
      return updated;
    });
  }, [currentIdx, advanceToNext]);

  const pendingCount = points.filter((p) => p.status === "pending").length;
  const doneCount = points.filter((p) => p.status === "done").length;

  return (
    <div className="relative w-full h-full min-h-[380px] flex items-center justify-center">
      {/* Phone mockup frame */}
      <div className="relative w-[260px] sm:w-[280px] aspect-[9/18] rounded-[2rem] border-[6px] border-space-600 bg-space-700 shadow-2xl overflow-hidden">
        {/* Status bar */}
        <div className="absolute top-0 left-0 right-0 h-7 bg-black/40 flex items-center justify-between px-4 z-20">
          <span className="text-[10px] text-white/80 font-medium">09:41</span>
          <div className="flex gap-1">
            <div className="w-3.5 h-1.5 rounded-sm bg-white/60" />
            <div className="w-1.5 h-1.5 rounded-full bg-white/60" />
          </div>
        </div>

        {/* Map area (schematic neon) */}
        <div className="absolute inset-0 bg-[#0a1220]">
          {/* Grid lines */}
          <svg className="absolute inset-0 w-full h-full opacity-30">
            <defs>
              <pattern
                id="grid"
                width="24"
                height="24"
                patternUnits="userSpaceOnUse"
              >
                <path
                  d="M 24 0 L 0 0 0 24"
                  fill="none"
                  stroke="rgba(0,229,255,0.15)"
                  strokeWidth="0.5"
                />
              </pattern>
            </defs>
            <rect width="100%" height="100%" fill="url(#grid)" />
          </svg>

          {/* Route path (dynamic) */}
          <svg className="absolute inset-0 w-full h-full">
            <motion.path
              d={`M ${points[0].x}% ${points[0].y}% L ${points[1].x}% ${points[1].y}% L ${points[2].x}% ${points[2].y}% L ${points[3].x}% ${points[3].y}%`}
              fill="none"
              stroke="url(#routeGradient)"
              strokeWidth="2"
              strokeDasharray="6 4"
              initial={{ pathLength: 0 }}
              animate={{ pathLength: 1 }}
              transition={{ duration: 1.5, ease: "easeInOut" }}
            />
            <defs>
              <linearGradient id="routeGradient" x1="0%" y1="0%" x2="100%" y2="0%">
                <stop offset="0%" stopColor="#00E5FF" />
                <stop offset="100%" stopColor="#A3E635" />
              </linearGradient>
            </defs>
          </svg>

          {/* Points on map */}
          {points.map((p) => (
            <motion.div
              key={p.id}
              className="absolute -translate-x-1/2 -translate-y-1/2"
              style={{ left: `${p.x}%`, top: `${p.y}%` }}
              animate={{
                scale: p.id === currentPoint?.id ? 1.2 : 1,
              }}
            >
              <div
                className={`flex h-6 w-6 items-center justify-center rounded-full border-2 text-[9px] font-bold ${
                  p.status === "done"
                    ? "bg-lime-neon border-lime-neon text-background"
                    : p.status === "skipped"
                    ? "bg-space-500 border-space-400 text-space-200"
                    : p.id === currentPoint?.id
                    ? "bg-cyan-electric border-cyan-electric text-background shadow-glow"
                    : "bg-surface border-cyan-electric/60 text-cyan-electric"
                }`}
              >
                {p.status === "done" ? (
                  <Check className="h-3 w-3" />
                ) : (
                  p.id
                )}
              </div>
            </motion.div>
          ))}

          {/* Current location pulse */}
          <div
            className="absolute -translate-x-1/2 -translate-y-1/2"
            style={{ left: "48%", top: "55%" }}
          >
            <span className="absolute inline-flex h-4 w-4 rounded-full bg-cyan-electric opacity-40 animate-ping" />
            <span className="relative inline-flex h-4 w-4 rounded-full bg-cyan-electric border-2 border-white" />
          </div>
        </div>

        {/* Floating Bubble Overlay */}
        <div className="absolute bottom-16 right-3 z-30">
          <AnimatePresence mode="wait">
            {bubbleState === "idle" && (
              <motion.div
                key="idle"
                initial={{ scale: 0.8, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.8, opacity: 0 }}
                className="flex flex-col items-end gap-2"
              >
                {/* Distance badge */}
                <div className="glass-strong rounded-full px-3 py-1 text-[10px] font-semibold text-cyan-electric border border-cyan-electric/30">
                  {distance} m
                </div>

                {/* Main bubble */}
                <motion.button
                  whileHover={{ scale: 1.05 }}
                  whileTap={{ scale: 0.95 }}
                  onClick={() => setShowList((v) => !v)}
                  className="relative flex h-14 w-14 items-center justify-center rounded-full bg-cyan-electric shadow-glow-strong border-2 border-white/20"
                  aria-label="Abrir bolha de ações"
                >
                  <MapPin className="h-6 w-6 text-background" strokeWidth={2.5} />
                  {pendingCount > 0 && (
                    <span className="absolute -top-1 -right-1 flex h-5 w-5 items-center justify-center rounded-full bg-lime-neon text-[10px] font-bold text-background">
                      {pendingCount}
                    </span>
                  )}
                </motion.button>

                {/* Action buttons */}
                <div className="flex gap-2">
                  <motion.button
                    whileHover={{ scale: 1.08 }}
                    whileTap={{ scale: 0.92 }}
                    onClick={handleConfirm}
                    disabled={!currentPoint || currentPoint.status !== "pending"}
                    className="flex h-10 w-10 items-center justify-center rounded-full bg-lime-neon text-background shadow-glow-lime disabled:opacity-40"
                    aria-label="Confirmar visita"
                  >
                    <Check className="h-5 w-5" strokeWidth={2.5} />
                  </motion.button>
                  <motion.button
                    whileHover={{ scale: 1.08 }}
                    whileTap={{ scale: 0.92 }}
                    onClick={handleSkip}
                    disabled={!currentPoint || currentPoint.status !== "pending"}
                    className="flex h-10 w-10 items-center justify-center rounded-full bg-space-500 text-white border border-space-400 disabled:opacity-40"
                    aria-label="Pular visita"
                  >
                    <SkipForward className="h-4 w-4" />
                  </motion.button>
                </div>
              </motion.div>
            )}

            {bubbleState === "confirm" && (
              <motion.div
                key="confirm"
                initial={{ scale: 0.5, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.5, opacity: 0 }}
                className="flex h-16 w-16 items-center justify-center rounded-full bg-lime-neon shadow-glow-lime"
              >
                <Check className="h-8 w-8 text-background" strokeWidth={3} />
              </motion.div>
            )}

            {bubbleState === "skip" && (
              <motion.div
                key="skip"
                initial={{ scale: 0.5, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                exit={{ scale: 0.5, opacity: 0 }}
                className="flex h-16 w-16 items-center justify-center rounded-full bg-space-400"
              >
                <SkipForward className="h-7 w-7 text-white" />
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Pending list overlay */}
        <AnimatePresence>
          {showList && (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 20 }}
              className="absolute inset-x-2 bottom-28 z-40 glass-strong rounded-xl p-3 border border-white/10"
            >
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-semibold text-white flex items-center gap-1.5">
                  <List className="h-3.5 w-3.5 text-cyan-electric" />
                  Pendentes ({pendingCount})
                </span>
                <button
                  onClick={() => setShowList(false)}
                  className="p-1 rounded hover:bg-white/10"
                  aria-label="Fechar lista"
                >
                  <X className="h-3.5 w-3.5 text-space-300" />
                </button>
              </div>
              <ul className="space-y-1.5 max-h-28 overflow-y-auto">
                {points
                  .filter((p) => p.status === "pending")
                  .map((p) => (
                    <li
                      key={p.id}
                      className={`flex items-center gap-2 text-[11px] px-2 py-1.5 rounded-lg ${
                        p.id === currentPoint?.id
                          ? "bg-cyan-electric/15 text-cyan-electric"
                          : "text-space-200"
                      }`}
                    >
                      <Navigation2 className="h-3 w-3 flex-shrink-0" />
                      <span className="truncate">{p.label}</span>
                    </li>
                  ))}
              </ul>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Bottom progress bar */}
        <div className="absolute bottom-0 left-0 right-0 h-10 bg-black/50 backdrop-blur-sm flex items-center justify-between px-4 z-20">
          <span className="text-[10px] text-space-200">
            {doneCount}/{points.length} concluídas
          </span>
          <div className="flex-1 mx-3 h-1.5 rounded-full bg-space-600 overflow-hidden">
            <motion.div
              className="h-full bg-gradient-to-r from-cyan-electric to-lime-neon rounded-full"
              initial={{ width: 0 }}
              animate={{
                width: `${(doneCount / points.length) * 100}%`,
              }}
              transition={{ duration: 0.4 }}
            />
          </div>
        </div>
      </div>

      {/* Hint text */}
      <p className="absolute -bottom-8 left-0 right-0 text-center text-xs text-space-300">
        Clique na bolha e nos botões para interagir
      </p>
    </div>
  );
}
