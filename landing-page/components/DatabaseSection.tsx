"use client";

import { useEffect, useRef, useState } from "react";
import { motion, useInView } from "framer-motion";
import { Database, Map, Zap, ShieldCheck } from "lucide-react";

function AnimatedCounter({
  target,
  suffix = "",
  decimals = 0,
}: {
  target: number;
  suffix?: string;
  decimals?: number;
}) {
  const [count, setCount] = useState(0);
  const ref = useRef<HTMLSpanElement>(null);
  const inView = useInView(ref, { once: true, margin: "-50px" });

  useEffect(() => {
    if (!inView) return;
    const duration = 2200;
    const start = performance.now();

    const tick = (now: number) => {
      const progress = Math.min((now - start) / duration, 1);
      // easeOutExpo
      const eased = progress === 1 ? 1 : 1 - Math.pow(2, -10 * progress);
      setCount(eased * target);
      if (progress < 1) requestAnimationFrame(tick);
    };
    requestAnimationFrame(tick);
  }, [inView, target]);

  return (
    <span ref={ref} className="tabular-nums">
      {count.toFixed(decimals).replace(".", ",")}
      {suffix}
    </span>
  );
}

const HIGHLIGHTS = [
  {
    icon: Database,
    title: "Dados oficiais IBGE",
    text: "Normalizados e auditáveis do CNEFE",
  },
  {
    icon: Zap,
    title: "Busca em 0,15 ms",
    text: "Local, sem latência de rede",
  },
  {
    icon: Map,
    title: "Custo zero de Geocoding",
    text: "Sem APIs pagas de geolocalização",
  },
  {
    icon: ShieldCheck,
    title: "Fallback inteligente",
    text: "Interpolação + Nominatim local",
  },
];

export default function DatabaseSection() {
  return (
    <section id="base-dados" className="section-padding relative overflow-hidden">
      {/* Background accent */}
      <div className="absolute inset-0 bg-gradient-to-b from-transparent via-cyan-electric/5 to-transparent pointer-events-none" />

      <div className="container-narrow relative">
        <div className="glass-strong rounded-3xl border border-white/10 p-8 sm:p-12 lg:p-16 overflow-hidden">
          {/* Decorative grid */}
          <div
            className="absolute inset-0 opacity-20 pointer-events-none"
            style={{
              backgroundImage:
                "linear-gradient(rgba(0,229,255,0.06) 1px, transparent 1px), linear-gradient(90deg, rgba(0,229,255,0.06) 1px, transparent 1px)",
              backgroundSize: "32px 32px",
            }}
          />

          <div className="relative grid lg:grid-cols-2 gap-12 items-center">
            {/* Left - Numbers */}
            <div>
              <motion.p
                initial={{ opacity: 0 }}
                whileInView={{ opacity: 1 }}
                viewport={{ once: true }}
                className="text-sm font-semibold tracking-widest text-cyan-electric uppercase mb-4"
              >
                Base de Dados Potente
              </motion.p>
              <motion.h2
                initial={{ opacity: 0, y: 12 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                className="text-3xl sm:text-4xl lg:text-5xl font-bold text-white leading-tight"
              >
                CNEFE IBGE
                <br />
                <span className="text-gradient">na palma da mão</span>
              </motion.h2>

              <div className="mt-10 space-y-6">
                <div>
                  <p className="text-5xl sm:text-6xl font-bold text-white tracking-tight">
                    <AnimatedCounter target={3.9} decimals={1} suffix="M" />
                  </p>
                  <p className="mt-1 text-space-200 text-sm">
                    Endereços em{" "}
                    <strong className="text-white">853 municípios</strong> de
                    Minas Gerais
                  </p>
                </div>
              </div>
            </div>

            {/* Right - Highlights */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {HIGHLIGHTS.map((item, i) => (
                <motion.div
                  key={item.title}
                  initial={{ opacity: 0, y: 16 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: i * 0.08 }}
                  className="rounded-xl border border-white/5 bg-surface/50 p-5 hover:border-cyan-electric/30 transition-colors"
                >
                  <item.icon className="h-5 w-5 text-cyan-electric mb-3" />
                  <h3 className="text-sm font-semibold text-white mb-1">
                    {item.title}
                  </h3>
                  <p className="text-xs text-space-300 leading-relaxed">
                    {item.text}
                  </p>
                </motion.div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}
