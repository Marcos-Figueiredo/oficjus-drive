"use client";

import { motion } from "framer-motion";
import {
  CircleDot,
  Route,
  RefreshCw,
  Mic,
  AlertTriangle,
  Zap,
} from "lucide-react";
import FloatingBubbleDemo from "./FloatingBubbleDemo";

const FEATURES = [
  {
    id: "bubble",
    title: "A Bolha Flutuante",
    subtitle: "WindowManager nativo",
    description:
      "Distância em tempo real, confirmar ou pular com um toque e modo chegada automática abaixo de 30 metros. Nunca mais saia do Waze.",
    icon: CircleDot,
    span: "md:col-span-2 md:row-span-2",
    highlight: true,
  },
  {
    id: "route",
    title: "Rota Dinâmica",
    subtitle: "Nearest-Neighbor",
    description:
      "Reotimização instantânea por GPS a cada parada concluída. A rota se adapta a você em tempo real.",
    icon: Route,
    span: "md:col-span-1",
  },
  {
    id: "eternal",
    title: "Rota Eterna",
    subtitle: "Zero perda",
    description:
      "Visitas não concluídas viram remanescentes automáticos para o próximo ciclo. Nada se perde.",
    icon: RefreshCw,
    span: "md:col-span-1",
  },
  {
    id: "voice",
    title: "Mãos Livres",
    subtitle: "Ditado inteligente",
    description:
      "Ditado por voz + Autocomplete ultraveloz que limpa redundâncias de logradouros e ordena por proximidade do GPS.",
    icon: Mic,
    span: "md:col-span-1",
  },
  {
    id: "duplicate",
    title: "Alerta de Duplicidade",
    subtitle: "Inteligência preventiva",
    description:
      "Detecta automaticamente endereços repetidos no mesmo trajeto e evita visitas desnecessárias.",
    icon: AlertTriangle,
    span: "md:col-span-1",
  },
];

export default function BentoGrid() {
  return (
    <section id="recursos" className="section-padding relative">
      <div className="container-narrow">
        <div className="text-center mb-14">
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-sm font-semibold tracking-widest text-cyan-electric uppercase mb-3"
          >
            Diferenciais competitivos
          </motion.p>
          <motion.h2
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-3xl sm:text-4xl font-bold text-white max-w-2xl mx-auto"
          >
            O core técnico que transforma
            <span className="text-gradient"> campo em fluxo</span>
          </motion.h2>
        </div>

        {/* Bento Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 lg:gap-5 auto-rows-fr">
          {/* Card Principal - Bolha */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className={`group relative glass rounded-2xl border-gradient overflow-hidden ${FEATURES[0].span} min-h-[420px] md:min-h-[480px]`}
          >
            <div className="absolute inset-0 bg-gradient-to-br from-cyan-electric/5 via-transparent to-lime-neon/5 opacity-0 group-hover:opacity-100 transition-opacity duration-500" />
            <div className="relative h-full flex flex-col p-6 lg:p-8">
              <div className="flex items-start gap-3 mb-4">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-cyan-electric/15 text-cyan-electric">
                  <CircleDot className="h-5 w-5" />
                </div>
                <div>
                  <h3 className="text-lg font-bold text-white">
                    {FEATURES[0].title}
                  </h3>
                  <p className="text-xs text-cyan-electric font-medium">
                    {FEATURES[0].subtitle}
                  </p>
                </div>
              </div>
              <p className="text-sm text-space-200 leading-relaxed mb-6 max-w-md">
                {FEATURES[0].description}
              </p>
              {/* Interactive Demo */}
              <div className="flex-1 flex items-center justify-center">
                <FloatingBubbleDemo />
              </div>
            </div>
          </motion.div>

          {/* Other cards */}
          {FEATURES.slice(1).map((feature, i) => (
            <motion.div
              key={feature.id}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: 0.1 + i * 0.08 }}
              className={`group relative glass rounded-2xl border-gradient p-6 lg:p-7 transition-all duration-300 hover:shadow-glow ${feature.span}`}
            >
              <div className="absolute inset-0 bg-gradient-to-br from-cyan-electric/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500 rounded-2xl" />
              <div className="relative">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-surface-elevated text-cyan-electric mb-4 group-hover:bg-cyan-electric/15 transition-colors">
                  <feature.icon className="h-5 w-5" />
                </div>
                <h3 className="text-base font-bold text-white mb-1">
                  {feature.title}
                </h3>
                <p className="text-xs text-cyan-electric/80 font-medium mb-2">
                  {feature.subtitle}
                </p>
                <p className="text-sm text-space-200 leading-relaxed">
                  {feature.description}
                </p>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
