"use client";

import { motion } from "framer-motion";
import { Code2, Layers, Server, Smartphone } from "lucide-react";

const STACK = [
  {
    category: "Linguagem & UI",
    icon: Smartphone,
    items: [
      { name: "Kotlin", detail: "100% Nativo" },
      { name: "Jetpack Compose", detail: "Material 3" },
    ],
  },
  {
    category: "Arquitetura & Local",
    icon: Layers,
    items: [
      { name: "Clean Architecture", detail: "MVVM" },
      { name: "Hilt", detail: "DI" },
      { name: "Room", detail: "SQLite local" },
    ],
  },
  {
    category: "Infra & Integração",
    icon: Server,
    items: [
      { name: "Supabase", detail: "Auth + PostgreSQL" },
      { name: "Waze SDK", detail: "Deep Link" },
      { name: "WindowManager", detail: "Overlay seguro" },
    ],
  },
];

export default function TechStack() {
  return (
    <section id="tecnologia" className="section-padding relative">
      <div className="container-narrow">
        <div className="text-center mb-14">
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-sm font-semibold tracking-widest text-cyan-electric uppercase mb-3"
          >
            Stack Tecnológica
          </motion.p>
          <motion.h2
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-3xl sm:text-4xl font-bold text-white"
          >
            Transparência técnica.
            <br />
            <span className="text-space-200 text-2xl sm:text-3xl font-medium">
              Performance de 24 MB.
            </span>
          </motion.h2>
        </div>

        {/* Hacker-style panel */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          className="glass rounded-2xl border border-white/10 overflow-hidden"
        >
          {/* Terminal header */}
          <div className="flex items-center gap-2 px-5 py-3 border-b border-white/5 bg-surface/40">
            <div className="flex gap-1.5">
              <span className="h-3 w-3 rounded-full bg-red-500/80" />
              <span className="h-3 w-3 rounded-full bg-yellow-500/80" />
              <span className="h-3 w-3 rounded-full bg-green-500/80" />
            </div>
            <span className="ml-3 text-xs font-mono text-space-300">
              oficjus-drive · architecture
            </span>
          </div>

          <div className="grid md:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-white/5">
            {STACK.map((group, i) => (
              <motion.div
                key={group.category}
                initial={{ opacity: 0 }}
                whileInView={{ opacity: 1 }}
                viewport={{ once: true }}
                transition={{ delay: i * 0.1 }}
                className="p-6 lg:p-8"
              >
                <div className="flex items-center gap-2.5 mb-5">
                  <group.icon className="h-4 w-4 text-cyan-electric" />
                  <h3 className="text-xs font-mono font-semibold tracking-wider text-cyan-electric uppercase">
                    {group.category}
                  </h3>
                </div>
                <ul className="space-y-3">
                  {group.items.map((item) => (
                    <li key={item.name} className="flex items-baseline gap-2">
                      <Code2 className="h-3.5 w-3.5 text-space-400 flex-shrink-0 mt-0.5" />
                      <div>
                        <span className="text-sm font-medium text-white">
                          {item.name}
                        </span>
                        <span className="text-xs text-space-300 ml-2">
                          {item.detail}
                        </span>
                      </div>
                    </li>
                  ))}
                </ul>
              </motion.div>
            ))}
          </div>
        </motion.div>
      </div>
    </section>
  );
}
