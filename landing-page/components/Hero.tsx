"use client";

import { motion } from "framer-motion";
import { Play, ArrowRight, WifiOff, Smartphone, Ban } from "lucide-react";

const METRICS = [
  { icon: Smartphone, label: "~24 MB APK" },
  { icon: WifiOff, label: "100% Offline" },
  { icon: Ban, label: "Sem Anúncios" },
];

export default function Hero() {
  return (
    <section className="relative min-h-screen flex items-center pt-20 pb-16 overflow-hidden">
      {/* Background grid */}
      <div
        className="absolute inset-0 opacity-40 pointer-events-none"
        style={{
          backgroundImage:
            "linear-gradient(rgba(0,229,255,0.04) 1px, transparent 1px), linear-gradient(90deg, rgba(0,229,255,0.04) 1px, transparent 1px)",
          backgroundSize: "48px 48px",
        }}
      />
      {/* Glow orbs */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-cyan-electric/10 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute bottom-1/4 right-1/4 w-80 h-80 bg-lime-neon/8 rounded-full blur-[100px] pointer-events-none" />

      <div className="container-narrow relative z-10 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto text-center">
          {/* Badge */}
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="inline-flex items-center gap-2 rounded-full border border-cyan-electric/30 bg-cyan-electric/10 px-4 py-1.5 mb-8"
          >
            <span className="relative flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-lime-neon opacity-75" />
              <span className="relative inline-flex rounded-full h-2 w-2 bg-lime-neon" />
            </span>
            <span className="text-xs font-medium text-cyan-soft tracking-wide">
              ANDROID NATIVO · 100% OFFLINE
            </span>
          </motion.div>

          {/* Headline */}
          <motion.h1
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.1 }}
            className="text-4xl sm:text-5xl lg:text-6xl font-bold tracking-tight text-white leading-[1.1]"
          >
            Não saia do{" "}
            <span className="text-gradient">Waze</span>
            <br />
            para registrar uma visita.
          </motion.h1>

          {/* Subheadline */}
          <motion.p
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.2 }}
            className="mt-6 text-lg sm:text-xl text-space-200 max-w-2xl mx-auto leading-relaxed"
          >
            Aplicativo Android nativo que otimiza suas rotas em campo{" "}
            <strong className="text-white">100% offline</strong>. Digite por
            voz, navegue com Waze e confirme tudo com nossa{" "}
            <strong className="text-cyan-electric">bolha flutuante exclusiva</strong>.
          </motion.p>

          {/* CTAs */}
          <motion.div
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4"
          >
            <a href="#preco" className="btn-primary text-base px-8 py-4 animate-pulse-neon group">
              Começar Teste Grátis
              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
            </a>
            <a href="#demo" className="btn-secondary text-base px-8 py-4 group">
              <Play className="h-4 w-4 text-cyan-electric" />
              Ver Demonstração
            </a>
          </motion.div>

          {/* Metrics badges */}
          <motion.div
            initial={{ opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.45 }}
            className="mt-14 flex flex-wrap items-center justify-center gap-3 sm:gap-4"
          >
            {METRICS.map((m) => (
              <div
                key={m.label}
                className="inline-flex items-center gap-2 rounded-full border border-space-500/60 bg-surface/60 backdrop-blur-sm px-4 py-2 text-sm text-space-200"
              >
                <m.icon className="h-4 w-4 text-cyan-electric" />
                <span className="font-medium">{m.label}</span>
              </div>
            ))}
          </motion.div>
        </div>
      </div>
    </section>
  );
}
